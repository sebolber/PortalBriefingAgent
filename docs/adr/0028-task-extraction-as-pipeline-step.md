# ADR 0028: Task extraction runs in the same pipeline as summary generation

* Status: accepted
* Date: 2026-05-13

## Context

Spec §6.2 describes task extraction as a parallel branch of the KI
pipeline. The Phase-1 completion report flagged that the
`TaskExtractionService` was still missing — tasks were only manually
creatable through `/api/tasks`. This ADR defines how the LLM-driven
extraction integrates with the rest of the pipeline now that it is
shipped.

## Decision

* Task extraction runs **after** summary generation as the last step
  of `EreignisService.runPipeline(...)`. Sequential execution keeps
  the request response coherent (the dashboard sees both summaries
  and tasks at the same time) and avoids races on the shared
  `EntityManager`.
* Each extracted candidate is persisted as a `Task` row with
  `status = open` and an initial `TaskStatusHistory` entry
  (`null → open`, note "extracted from transcript"). The author then
  uses the existing `/tasks` page (or future review-screen tasks tab)
  to edit, drop or complete them.
* Assignment-string parsing follows the spec's edge case rule: the
  LLM may emit `self`, `person:<uuid>`, `persongroup:<uuid>` or
  `topic:<uuid>`. Anything we cannot resolve cleanly (unknown UUID,
  missing colon, foreign type) falls back to
  `assignedToSelf = true`.
* Malformed JSON, missing titles and unparseable due dates are
  swallowed silently — the pipeline never fails the capture because
  of a flaky extractor response.

## Rationale

* Per ADR 0012 the pipeline is synchronous in phase 1 for test
  determinism; running task extraction inside the same transaction
  preserves that property.
* Auto-saving with `status = open` instead of returning candidates
  for explicit review keeps the workflow simple at this iteration's
  scope; the audit trail still lets the author trace where each task
  came from. The richer "review the candidates first" UX from
  Spec §6.3 is a Phase 2 enhancement.

## Consequences

* The pipeline now persists summaries **and** tasks for every Ereignis;
  the existing `/tasks` page surfaces extractions immediately.
* The mock LLM keeps returning `{"tasks":[]}` for the
  `TASK_EXTRACTION` purpose so tests and demos see deterministic
  no-op extractions.
* When the explicit review-screen tasks tab lands in Phase 2, it can
  filter on `Task.ereignis_id` and the audit-trail entry produced
  here.

## Alternatives

* Skip auto-persist; stage candidates somewhere else for explicit
  review — adds an extra entity (`TaskCandidate`) for marginal value.
* Run task extraction asynchronously — would force a status-tracking
  mechanism that does not exist yet.
