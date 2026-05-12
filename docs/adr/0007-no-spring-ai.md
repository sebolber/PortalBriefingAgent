# ADR 0007: Use Spring RestClient instead of Spring AI

* Status: accepted
* Date: 2026-05-12

## Context

The spec specifies that LLM and STT providers are configured at runtime
(table `llm_provider`) and that the system speaks the OpenAI-compatible
chat-completions and audio-transcriptions APIs. Spring AI offers a
provider abstraction but baked in at compile time and module-scoped.

## Decision

Build a thin `LlmClient` / `SttProviderClient` abstraction on top of
**Spring `RestClient`**. Do not depend on `spring-ai-*`.

## Rationale

* Providers are dynamic — defined in the database, switched per purpose
  at runtime. Spring AI's auto-configured client beans would conflict
  with this model.
* The actual transport is a plain HTTPS POST with JSON. The surface
  area is small enough that a hand-rolled adapter is easier to reason
  about and to test (WireMock).
* Spring AI is still maturing as of May 2026; pinning to it would tie
  the project to its lifecycle.

## Consequences

* JSON schemas for classification / summary / task extraction are
  validated manually in the service layer.
* Adding a new provider type means writing a small adapter, not
  upgrading the Spring AI version.

## Alternatives

* Spring AI 1.x — rejected for the reasons above. Revisit in phase 2 if
  it stabilises.
* OkHttp / Apache HttpClient directly — possible but redundant; Boot
  already pulls in `RestClient`.
