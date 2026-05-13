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
# and only installs the things that are missing. Linux and macOS are
# both supported; Linux installs go through apt-get (Debian/Ubuntu),
# macOS installs go through Homebrew.

set -euo pipefail
IFS=$'\n\t'

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${REPO_ROOT}/backend"
FRONTEND_DIR="${REPO_ROOT}/frontend"
STATIC_TARGET="${BACKEND_DIR}/src/main/resources/static"

REQUIRED_JAVA_MAJOR=21
REQUIRED_NODE_MAJOR=20

PLATFORM="$(uname -s)"

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

ensure_brew() {
  if command -v brew >/dev/null 2>&1; then
    return
  fi
  fail "Homebrew not found. Install it from https://brew.sh and re-run this script."
}

# Cross-platform package install. First argument is the package name on
# apt-get; subsequent arguments are brew formula/cask hints in the form
# "formula:<name>" or "cask:<name>". The first brew hint that matches the
# platform wins.
pkg_install() {
  local apt_pkg="$1"; shift
  if [[ "${PLATFORM}" == "Darwin" ]]; then
    ensure_brew
    local hint
    for hint in "$@"; do
      case "${hint}" in
        formula:*)
          local formula="${hint#formula:}"
          log "Installing ${formula} via brew…"
          brew install "${formula}"
          return
          ;;
        cask:*)
          local cask="${hint#cask:}"
          log "Installing ${cask} via brew cask…"
          brew install --cask "${cask}"
          return
          ;;
      esac
    done
    fail "No brew hint provided for ${apt_pkg} — please install it manually."
  elif command -v apt-get >/dev/null 2>&1; then
    if is_root; then
      apt-get update -qq
      apt-get install -y --no-install-recommends "${apt_pkg}"
    else
      sudo apt-get update -qq
      sudo apt-get install -y --no-install-recommends "${apt_pkg}"
    fi
  else
    fail "Unsupported platform (${PLATFORM}). Install ${apt_pkg} manually and re-run."
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
  pkg_install "openjdk-${REQUIRED_JAVA_MAJOR}-jdk-headless" "formula:openjdk@${REQUIRED_JAVA_MAJOR}"
  if [[ "${PLATFORM}" == "Darwin" ]]; then
    log "On macOS you may need to add openjdk@${REQUIRED_JAVA_MAJOR} to your PATH (brew prints the snippet)."
  fi
}

ensure_maven() {
  if command -v mvn >/dev/null 2>&1; then
    log "Maven $(mvn -v | head -1)"
    return
  fi
  log "Installing Maven…"
  pkg_install maven "formula:maven"
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
  if [[ "${PLATFORM}" == "Darwin" ]]; then
    log "Installing Node.js ${REQUIRED_NODE_MAJOR}.x via brew…"
    ensure_brew
    brew install "node@${REQUIRED_NODE_MAJOR}"
    log "Add node@${REQUIRED_NODE_MAJOR} to your PATH if brew prints a hint (it is keg-only)."
  else
    log "Installing Node.js ${REQUIRED_NODE_MAJOR}.x via NodeSource…"
    if is_root; then
      curl -fsSL "https://deb.nodesource.com/setup_${REQUIRED_NODE_MAJOR}.x" | bash -
      apt-get install -y nodejs
    else
      curl -fsSL "https://deb.nodesource.com/setup_${REQUIRED_NODE_MAJOR}.x" | sudo -E bash -
      sudo apt-get install -y nodejs
    fi
  fi
}

ensure_docker() {
  if command -v docker >/dev/null 2>&1; then
    log "Docker present: $(docker --version)"
  else
    if [[ "${PLATFORM}" == "Darwin" ]]; then
      fail "Docker not found. Install Docker Desktop from https://www.docker.com/products/docker-desktop/ and re-run."
    fi
    log "Installing Docker engine…"
    pkg_install docker.io || fail "Could not install Docker. Please install it manually."
  fi
  if ! docker info >/dev/null 2>&1; then
    if [[ "${PLATFORM}" == "Darwin" ]]; then
      fail "Docker daemon is not running. Start Docker Desktop and re-run."
    fi
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

  local before after
  before="$(git rev-parse HEAD)"
  log "Pulling latest commits on ${BRANCH}…"
  if git pull --ff-only origin "${BRANCH}"; then
    after="$(git rev-parse HEAD)"
    if [[ "${before}" != "${after}" ]]; then
      log "Pulled ${before:0:7} → ${after:0:7}. Re-executing with the fresh code…"
      export BA_RUN_AFTER_PULL=1
      exec "${BASH_SOURCE[0]}" "${BRANCH}"
    fi
    log "Already at the latest commit (${after:0:7})."
  else
    log "(No upstream pull performed; staying on current commit.)"
  fi
}

# ---------------------------------------------------------------------------
# Database + Whisper.
# ---------------------------------------------------------------------------

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose -f "${REPO_ROOT}/docker-compose.yml" "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose -f "${REPO_ROOT}/docker-compose.yml" "$@"
  else
    fail "Neither 'docker compose' nor 'docker-compose' is available."
  fi
}

start_database() {
  log "Starting PostgreSQL via docker compose…"
  compose_cmd up -d db

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

start_whisper() {
  if [[ "${BA_SKIP_WHISPER:-0}" == "1" ]]; then
    log "BA_SKIP_WHISPER=1 — Whisper container not started (audio capture will fail until you configure an STT provider)."
    return
  fi

  log "Starting local Whisper (OpenAI-compatible API on :9000) via docker compose…"
  compose_cmd up -d whisper

  log "Waiting for Whisper to accept connections…"
  local attempts=60
  until docker exec briefing-agent-whisper curl -fsS http://localhost:8000/v1/models >/dev/null 2>&1; do
    attempts=$((attempts - 1))
    if [[ "${attempts}" -le 0 ]]; then
      log "Whisper did not respond within 5 minutes — continuing anyway."
      log "Re-run later or set BA_SKIP_WHISPER=1 if you do not need audio capture."
      return
    fi
    sleep 5
  done
  log "Whisper is up."

  local model="${WHISPER_MODEL:-Systran/faster-whisper-small}"
  if docker exec briefing-agent-whisper curl -fsS "http://localhost:8000/v1/models" 2>/dev/null \
      | grep -q "\"${model}\""; then
    log "Whisper model '${model}' is already loaded."
    return
  fi

  log "Pre-pulling Whisper model '${model}' (one-time download, ~500 MB for the 'small' checkpoint)…"
  if docker exec briefing-agent-whisper curl -fsS -X POST \
        -H 'Content-Type: application/json' \
        -d "{\"id\":\"${model}\"}" \
        "http://localhost:8000/v1/models" >/dev/null 2>&1; then
    log "Whisper model download triggered."
  else
    log "Whisper preload call failed — the first audio capture will trigger the download instead."
  fi
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
# Encryption master key. Persisted per host so DB ciphertexts stay readable
# across restarts. Operators can override by exporting BRIEFINGAGENT_SECRET_KEY
# explicitly (e.g. from a real secret store).
# ---------------------------------------------------------------------------

ensure_secret_key() {
  if [[ -n "${BRIEFINGAGENT_SECRET_KEY:-}" ]]; then
    log "Using BRIEFINGAGENT_SECRET_KEY from the environment."
    return
  fi
  local dir="${HOME}/.briefingagent"
  local key_file="${dir}/secret-key"
  mkdir -p "${dir}"
  chmod 700 "${dir}"
  if [[ -s "${key_file}" ]]; then
    BRIEFINGAGENT_SECRET_KEY="$(cat "${key_file}")"
    export BRIEFINGAGENT_SECRET_KEY
    log "Loaded host encryption key from ${key_file}."
    return
  fi
  log "Generating new 256-bit encryption master key at ${key_file}…"
  local generated
  generated="$(head -c 32 /dev/urandom | base64)"
  (umask 077 && printf '%s' "${generated}" > "${key_file}")
  chmod 600 "${key_file}"
  BRIEFINGAGENT_SECRET_KEY="${generated}"
  export BRIEFINGAGENT_SECRET_KEY
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
  log "Platform: ${PLATFORM}"
  if [[ "${BA_RUN_AFTER_PULL:-}" == "1" ]]; then
    log "Continuing after re-exec; skipping the second git pull."
  else
    ensure_branch
  fi
  ensure_java
  ensure_maven
  ensure_node
  ensure_docker
  ensure_secret_key
  start_database
  start_whisper
  build_frontend
  stage_frontend_bundle
  build_backend
  run_backend
}

main "$@"
