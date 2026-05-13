# ADR 0019: Prompt templates per author with monotonic versioning

* Status: accepted
* Date: 2026-05-12

## Context

Spec §7a.2 mandates per-author prompts with version history. Concurrent
writes (e.g. two open browser tabs) must not produce two `active=true`
rows for the same `(author, purpose)` pair, and restoring a previous
version must be straightforward.

## Decision

* Each save creates a new row with `version = max + 1` and
  `active = true`; previous rows for the same `(author, purpose)` are
  updated to `active = false` in the same transaction.
* The schema enforces "one active per (author, purpose)" via a partial
  unique index, so a race between two writers fails the slower one
  with a `DataIntegrityViolationException` instead of corrupting the
  state.
* Restoration re-activates the requested version and de-activates the
  current one symmetrically.
* `PromptPlaceholders` validates required `{{...}}` placeholders before
  the row is saved; missing placeholders surface as HTTP 400 with the
  list of missing names.

## Rationale

* Versioning gives the author a free A/B-tracking history.
* Doing the de-activation explicitly in the service keeps the
  transaction boundary visible; relying on the partial index alone
  would force the writer to handle concurrent constraint violations
  at every call site.

## Consequences

* History queries are cheap thanks to the
  `(author_id, purpose, version DESC)` index.
* "Restore" is a one-click operation; no destructive copy needed.
* Stored prompts are validated for placeholders, but the runtime
  rendering still uses an explicit placeholder map (no eval).

## Alternatives

* Soft-delete previous versions — rejected; we want to be able to
  look up old prompts in the audit trail.
* JSONB array of versions on a single row — rejected; harder to
  query and to enforce uniqueness.
