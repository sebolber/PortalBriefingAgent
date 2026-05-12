# ADR 0013: Graceful JSON parsing for LLM responses

* Status: accepted
* Date: 2026-05-12

## Context

LLMs frequently return malformed JSON, wrap their output in Markdown
fences, hallucinate UUIDs, or invent fields. The classifier in
particular has to consume model output reliably.

## Decision

Implement a lenient parser in
`AudienceClassificationService.parse(...)` that:

* Treats any parse failure as "no matches" rather than an exception.
* Filters entries whose `id` is not a parsable UUID.
* Filters entries whose `id` does not exist among the author's
  audiences.
* Filters entries whose `type` does not match the resolved audience's
  actual type.
* Maps unknown `confidence` strings to `low` (the safest default).
* Returns an immutable list ordered exactly as the model emitted it.

No JSON-Schema library is involved; the surface area is small enough
that hand-written checks are more transparent.

## Rationale

* Phase 1 prefers correctness over throughput. We would rather drop a
  spurious entry than throw a 500 to the user.
* The synchronous pipeline (ADR 0012) wraps the call in a transaction;
  raising would roll back the whole capture, including the
  successfully created Ereignis. Returning an empty list lets the
  fallback summary kick in.
* Dropping entries is logged at DEBUG so the behaviour stays
  diagnosable in development without being noisy in production.

## Consequences

* Tests assert each rejection path independently (malformed JSON,
  unknown UUID, type mismatch, bogus confidence). See
  `AudienceClassificationServiceTest`.
* When we add provider-aware retries (Iter 5) the retry policy will
  layer on top of this parser.

## Alternatives

* JSON-Schema validation (e.g. `everit-org/json-schema`) — rejected;
  more dependencies and stricter error reporting than this iteration
  needs. Can be revisited when the provider table arrives.
