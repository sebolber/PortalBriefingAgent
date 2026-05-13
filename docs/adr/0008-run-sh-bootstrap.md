# ADR 0008: `scripts/run.sh` as the single-command launcher

* Status: accepted
* Date: 2026-05-12

## Context

The requester wants a single script that, given a branch name,
checks out the branch, installs missing tooling, builds the project and
starts it. Two flavours were considered: a portable Bash script, or a
heavier Python / Node CLI.

## Decision

Provide `scripts/run.sh`, a Bash script with:

* `set -euo pipefail` for strict execution.
* Idempotent tool detection (Java 21, Maven, Node 20+, Docker).
* `apt-get` installs as a best-effort fallback when tools are missing
  and the script has the rights to install.
* Frontend bundle is staged into
  `backend/src/main/resources/static/` so a single Spring Boot process
  serves both the SPA and the API.

## Rationale

* Bash is everywhere on Linux servers; no extra runtime is required.
* The script's job is mostly to call other tools (`mvn`, `npm`,
  `docker`), so a thin shell wrapper is sufficient.
* Serving the SPA from Spring Boot avoids managing two processes in
  development and keeps the same-origin guarantee that the CSRF cookie
  flow relies on.

## Consequences

* The script assumes a Debian/Ubuntu host for the auto-install path.
  Other distributions need to install the tooling manually before
  running the script.
* The first run downloads Maven and npm dependencies; subsequent runs
  reuse the caches.

## Alternatives

* Makefile — would require GNU Make to be present and is awkward for
  the branching workflow.
* Dedicated CLI in Node — adds Node to the bootstrap path before Node
  itself is even installed.
