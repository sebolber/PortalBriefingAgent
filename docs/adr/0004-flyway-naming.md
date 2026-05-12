# ADR 0004: Flyway versioned migrations with `V{YYYYMMDDhhmm}` prefixes

* Status: accepted
* Date: 2026-05-12

## Context

The spec mandates Flyway. We need a stable, conflict-free numbering scheme
that survives merges from independent feature branches.

## Decision

* Migration files live in
  `backend/src/main/resources/db/migration/`.
* File names follow the pattern
  `V{YYYYMMDDhhmm}__{snake_case_description}.sql`
  (UTC timestamp, four-digit minute precision).
* Repeatable migrations (seed data, default prompts) use the prefix
  `R__{description}.sql`.

## Rationale

* Timestamps avoid merge conflicts when two branches each add a
  migration in parallel.
* Sequential integers would force every cross-team migration to be
  renumbered manually.
* Repeatable migrations seed the same data on every boot and are easy to
  reason about for tests.

## Consequences

* Migrations are immutable once they ship. Schema changes happen via
  follow-up `V` migrations.
* The `flyway-database-postgresql` module is included so Flyway 10+ can
  introspect PostgreSQL fully.

## Alternatives

* Liquibase — bigger surface area, two file formats (YAML/XML/SQL);
  Flyway covers what Briefing Agent needs.
