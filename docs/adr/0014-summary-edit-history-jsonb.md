# ADR 0014: Summary edit history as append-only JSONB

* Status: accepted
* Date: 2026-05-12

## Context

Spec §6.3 requires that every change to a generated summary leave an
audit-trail entry: who edited what, when, and (for regenerations)
which feedback was given. The trail must be reconstructable years
later.

## Decision

* Add a single JSONB column `summary.edit_history` (default `'[]'`),
  appended to once per change.
* Map it in JPA via Hibernate's `@JdbcTypeCode(SqlTypes.JSON)` so the
  type bridges PostgreSQL's `jsonb` to a Java `List<EditHistoryEntry>`
  without requiring an extra dependency such as `hypersistence-utils`.
* Enforce append-only semantics in the service layer, not the schema:
  `SummaryReviewService.appendHistory(...)` is the only writer; tests
  assert that the latest entry is appended at the end.
* Cover the column with a GIN index so future queries (e.g. "all
  summaries that were regenerated") stay efficient.

## Rationale

* JSONB keeps each entry self-contained and avoids a separate
  `summary_edit_history` table whose only consumer would be the audit
  view.
* The history is rendered chronologically in the review UI; relational
  joins would offer no extra value.
* Hibernate's built-in JSON SQL type avoids pulling in third-party
  hibernate-types just to map a single column.

## Consequences

* The migration is PostgreSQL-only (`jsonb`). Tests that need the
  schema run against Testcontainers; unit tests mock the persistence
  layer.
* Querying inside the JSONB array (e.g. "summaries edited by user X")
  is possible via PostgreSQL operators; not used in phase 1.

## Alternatives

* Separate child table — rejected; unnecessary join surface for a
  log-style append.
* `hypersistence-utils` library — rejected; native Hibernate annotation
  covers the case without extra deps.
