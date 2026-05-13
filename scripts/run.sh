#!/usr/bin/env bash
# Briefing Agent launch script.
#
# Usage: scripts/run.sh <branch>
#
# Checks for required toolchains (Java 21, Maven, Node 20+, npm, Docker),
# installs the missing pieces where it can, checks out the given branch,
# starts the local PostgreSQL container, builds backend and frontend, and
# launches the Spring Boot backend with the Angular bundle served as static
# resources.
#
# The script is idempotent: it leaves existing tools and containers alone
# and only installs the things that are missing.

set -euo pipefail
IFS=$'\n\t'

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${REPO_ROOT}/backend"
FRONTEND_DIR="${REPO_ROOT}/frontend"
STATIC_TARGET="${BACKEND_DIR}/src/main/resources/static"

REQUIRED_JAVA_MAJOR=21
REQUIRED_NODE_MAJOR=20

log() { printf '[run] %s\n' "$*" >&2; }
fail() { printf '[run] ERROR: %s\n' "$*" >&2; exit 1; }

usage() {
  cat <<'USAGE' >&2
Briefing Agent launcher
-----------------------
Usage: scripts/run.sh <branch>

  <branch>   Required. The Git branch to check out, build and run.

Examples:
  scripts/run.sh main
  scripts/run.sh claude/briefing-agent-phase1-SHTMm
USAGE
  exit 2
}

[[ $# -eq 1 ]] || usage
BRANCH="$1"
[[ -n "${BRANCH}" ]] || usage

trap 'log "Aborted (exit $?)"; exit $?' ERR

# ---------------------------------------------------------------------------
# Toolchain checks and (best-effort) installation.
# ---------------------------------------------------------------------------

is_root() {
  [[ "$(id -u)" -eq 0 ]]
}

apt_install() {
  local pkgs=("$@")
  if command -v apt-get >/dev/null 2>&1; then
    if is_root; then
      apt-get update -qq
      apt-get install -y --no-install-recommends "${pkgs[@]}"
    else
      sudo apt-get update -qq
      sudo apt-get install -y --no-install-recommends "${pkgs[@]}"
    fi
  else
    fail "apt-get not available and the following packages are missing: ${pkgs[*]}"
  fi
}

ensure_java() {
  if command -v java >/dev/null 2>&1; then
    local version
    version="$(java -version 2>&1 | awk -F\" '/version/ {print $2}' | head -1)"
    local major="${version%%.*}"
    if [[ "${major}" -ge "${REQUIRED_JAVA_MAJOR}" ]]; then
      log "Java ${version} already installed."
      return
    fi
  fi
  log "Installing OpenJDK ${REQUIRED_JAVA_MAJOR}…"
  apt_install "openjdk-${REQUIRED_JAVA_MAJOR}-jdk-headless" || \
    fail "Could not install OpenJDK ${REQUIRED_JAVA_MAJOR}. Please install it manually."
}

ensure_maven() {
  if command -v mvn >/dev/null 2>&1; then
    log "Maven $(mvn -v | head -1)"
    return
  fi
  log "Installing Maven…"
  apt_install maven || fail "Could not install Maven. Please install it manually."
}

ensure_node() {
  if command -v node >/dev/null 2>&1; then
    local version major
    version="$(node --version | sed 's/^v//')"
    major="${version%%.*}"
    if [[ "${major}" -ge "${REQUIRED_NODE_MAJOR}" ]]; then
      log "Node.js v${version} already installed."
      return
    fi
    log "Node ${version} is older than v${REQUIRED_NODE_MAJOR} — installing a newer line."
  fi
  log "Installing Node.js ${REQUIRED_NODE_MAJOR}.x via NodeSource…"
  if is_root; then
    curl -fsSL "https://deb.nodesource.com/setup_${REQUIRED_NODE_MAJOR}.x" | bash -
    apt-get install -y nodejs
  else
    curl -fsSL "https://deb.nodesource.com/setup_${REQUIRED_NODE_MAJOR}.x" | sudo -E bash -
    sudo apt-get install -y nodejs
  fi
}

ensure_docker() {
  if command -v docker >/dev/null 2>&1; then
    log "Docker present: $(docker --version)"
  else
    log "Installing Docker engine…"
    apt_install docker.io || fail "Could not install Docker. Please install it manually."
  fi
  if ! docker info >/dev/null 2>&1; then
    fail "Docker daemon is not reachable. Start it (e.g. 'sudo systemctl start docker') and re-run."
  fi
}

# ---------------------------------------------------------------------------
# Git workflow.
# ---------------------------------------------------------------------------

ensure_branch() {
  cd "${REPO_ROOT}"
  log "Fetching origin…"
  git fetch --all --prune --tags
  if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
    git checkout "${BRANCH}"
  elif git ls-remote --exit-code --heads origin "${BRANCH}" >/dev/null 2>&1; then
    git checkout -B "${BRANCH}" "origin/${BRANCH}"
  else
    fail "Branch '${BRANCH}' exists neither locally nor on origin."
  fi
  log "Pulling latest commits on ${BRANCH}…"
  git pull --ff-only origin "${BRANCH}" || log "(No upstream pull performed; staying on current commit.)"
}

# ---------------------------------------------------------------------------
# Database.
# ---------------------------------------------------------------------------

start_database() {
  log "Starting PostgreSQL via docker compose…"
  if docker compose version >/dev/null 2>&1; then
    docker compose -f "${REPO_ROOT}/docker-compose.yml" up -d db
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose -f "${REPO_ROOT}/docker-compose.yml" up -d db
  else
    fail "Neither 'docker compose' nor 'docker-compose' is available."
  fi

  log "Waiting for the database to accept connections…"
  local attempts=30
  until docker exec briefing-agent-db pg_isready -U briefing_agent -d briefing_agent >/dev/null 2>&1; do
    attempts=$((attempts - 1))
    if [[ "${attempts}" -le 0 ]]; then
      fail "PostgreSQL did not become ready within 60 seconds."
    fi
    sleep 2
  done
  log "Database is ready."
}

# ---------------------------------------------------------------------------
# Build steps.
# ---------------------------------------------------------------------------

build_frontend() {
  log "Installing frontend dependencies…"
  cd "${FRONTEND_DIR}"
  npm ci --no-audit --no-fund
  log "Building frontend production bundle…"
  npm run build
}

stage_frontend_bundle() {
  log "Staging frontend bundle as Spring Boot static resources…"
  rm -rf "${STATIC_TARGET}"
  mkdir -p "${STATIC_TARGET}"
  local browser_dir="${FRONTEND_DIR}/dist/briefing-agent/browser"
  if [[ -d "${browser_dir}" ]]; then
    cp -r "${browser_dir}/." "${STATIC_TARGET}/"
  else
    cp -r "${FRONTEND_DIR}/dist/briefing-agent/." "${STATIC_TARGET}/"
  fi
}

build_backend() {
  log "Building backend (Maven)…"
  cd "${BACKEND_DIR}"
  mvn -q -DskipTests -Pskip-integration-tests clean package
}

# ---------------------------------------------------------------------------
# Launch.
# ---------------------------------------------------------------------------

run_backend() {
  log "Launching Spring Boot (http://localhost:8080)…"
  cd "${BACKEND_DIR}"
  local jar
  jar="$(ls target/briefing-agent-backend.jar 2>/dev/null || true)"
  [[ -n "${jar}" ]] || fail "Backend jar not found in target/."
  exec java -jar "${jar}" --spring.profiles.active=dev
}

main() {
  ensure_java
  ensure_maven
  ensure_node
  ensure_docker
  ensure_branch
  start_database
  build_frontend
  stage_frontend_bundle
  build_backend
  run_backend
}

main "$@"
