# Briefing Agent

Personal briefing tool. The author captures recordings (text or audio) of
meetings, workshops or sales events. The Briefing Agent transcribes audio,
classifies the captured content per audience, generates audience-specific
summaries and extracts tasks. Before the next meeting, the author opens the
dashboard and pulls the relevant briefing.

The specification (in German) lives at
[`docs/briefing-agent-phase1-spec.md`](docs/briefing-agent-phase1-spec.md).
The iteration plan is in [`docs/iteration-plan.md`](docs/iteration-plan.md).

## Repository layout

| Path | Contents |
|------|----------|
| `backend/` | Spring Boot 3 backend (Java 21, Maven, Flyway, JPA). |
| `frontend/` | Angular 18 web frontend (standalone components, signals). |
| `docs/` | Specification, iteration plan, ADRs, completion reports. |
| `scripts/run.sh` | One-shot launcher that installs missing tooling, checks out the requested branch, builds backend + frontend and starts the app. |
| `docker-compose.yml` | Local PostgreSQL 16 used in development and Testcontainers. |

## Quick start

```bash
scripts/run.sh <branch>
```

The script accepts a Git branch as parameter. It will:

1. Verify Java 21, Maven, Node 20+, npm and Docker are installed; attempt to
   install anything that is missing via `apt-get` where it has the rights.
2. Check out the given branch (creating it from the remote if necessary).
3. Start PostgreSQL via Docker Compose (`briefing_agent` database, `briefing_agent` user, password `briefing_agent`).
4. Build the frontend (`npm ci && npm run build`) and stage the production
   bundle under `backend/src/main/resources/static/`.
5. Build the backend (`mvn clean package -DskipTests`).
6. Launch the Spring Boot backend on <http://localhost:8080>.

Once the backend is running, open <http://localhost:8080>. A dev author is
seeded on first start: username `demo`, password `demo-password-change-me`.
Change the password before any non-local use.

## Manual development workflow

### Backend

```bash
cd backend
mvn -Pskip-integration-tests test            # fast unit tests, no Docker
mvn verify                                   # full suite incl. Testcontainers
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

* Unit tests live next to the code (`*Test.java`) and run with Surefire.
* Integration tests are tagged by file name (`*IT.java`), run with
  Failsafe, and need Docker for Testcontainers. The
  `skip-integration-tests` profile suppresses them when Docker is not
  available.

### Frontend

```bash
cd frontend
npm ci
npm run lint
npm run test:ci                              # headless Chrome + coverage
npm start                                    # dev server with /api proxy
```

The dev server proxies `/api/*` and `/actuator/*` to
`http://localhost:8080` (see `frontend/proxy.conf.json`).

Karma needs a Chromium/Chrome binary on the host. The launcher script
installs it where it has package-manager rights; otherwise install
`chromium` or `google-chrome-stable` manually.

## Configuration

| Property | Where | Default |
|----------|-------|---------|
| Database URL / user / password | `spring.datasource.*` (`application-dev.yml` / env vars) | `localhost:5432/briefing_agent`, `briefing_agent` / `briefing_agent` |
| BCrypt cost | `briefingagent.security.bcrypt-strength` | `12` (production minimum; the schema validates `>=10`) |
| Mock LLM | `briefingagent.llm.mock.enabled` | `true` (Iter 0 walking skeleton) |
| Server port | `server.port` | `8080` |

Secrets must be supplied via environment variables in production
(`DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`).

## Phase 1 scope

Phase 1 ships the web stack (this repository). The native iOS/Mac client
is planned for a later phase and is outside the scope of this codebase.
See `docs/iteration-plan.md` for an iteration-by-iteration breakdown of
what is delivered.

## Branding

UI colours follow the corporate-design hex codes (primary
`#006ec7`) but are exposed under neutral CSS variable names such as
`--color-brand-primary` so the same structure can host any palette without
renaming.
