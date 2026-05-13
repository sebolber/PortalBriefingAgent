# ADR 0012: Synchronous pipeline with a default-topic fallback

* Status: accepted
* Date: 2026-05-12

## Context

Iter 2 introduces the multi-shot pipeline (classify → summary per
audience). The original plan called for asynchronous execution via
Spring Events + `@Async`. We have to balance two concerns:

* **Determinism in tests**: HTTP request handlers should not return
  before the resulting summaries are persisted, otherwise the dashboard
  tests would race with the pipeline.
* **Robustness when the classifier returns nothing**: the spec is silent
  on what should happen if the LLM finds no relevant audience.

## Decision

* The capture endpoints run the pipeline **synchronously** for phase 1.
  The HTTP response includes the resulting summaries, which makes the
  request/response contract observable end-to-end.
* If the classifier returns an empty match list, the service falls back
  to the author's default topic ("My Notes") with confidence `low` and
  a clear reasoning string ("Keine relevante Audience erkannt …").

## Rationale

* For phase-1 traffic (one author at a time, a handful of audiences) the
  pipeline finishes in a few hundred milliseconds. Asynchronous
  execution adds eventual-consistency complexity that we do not need.
* A guaranteed fallback summary preserves the "every event leaves a
  trace" property of the dashboard. Without it, an empty classification
  would leave the user staring at a transcript with no summary at all.
* When real-world latency forces async, switching to
  `EreignisAcceptedEvent` + `@Async` is a small refactor: the pipeline
  service already has a single entry point (`runPipeline`).

## Consequences

* `EreignisService.captureText/Audio` waits for classification + multi-
  shot summary generation before returning.
* The fallback summary is stored as an ordinary `Summary` row attached
  to the default Topic; it is therefore retained, edited and rendered
  exactly like any other summary.
* Future iterations will revisit this when the LLM provider table and
  retries land — the @Async transition will be its own ADR.

## Alternatives

* Async from day one — rejected for the test-determinism and
  fallback-clarity reasons above.
* No fallback — rejected; the dashboard would silently swallow events
  and the spec's "Personal Briefing Tool" framing requires that every
  capture produce something the author can review.
