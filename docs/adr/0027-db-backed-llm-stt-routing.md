# ADR 0027: DB-backed LLM/STT routing with property fallback

* Status: accepted
* Date: 2026-05-13

## Context

Iter 5 introduced the `llm_provider`, `llm_provider_usage`,
`stt_provider` and `prompt_template` tables plus their REST surface.
The Phase-1 completion report correctly flagged that the live
LLM/STT calls were still pulling their endpoint from
`application.yml` — the tables were the editing surface, but not yet
the source of truth at request time. This ADR closes that gap.

## Decision

* A new `DbBackedLlmClient` is registered as the `@Primary`
  implementation of `LlmClient`. For every request:
  1. Look up the active `llm_provider_usage` row for the request's
     `LlmPurpose`.
  2. If found, dispatch to the provider's endpoint via
     `HttpChatCompletionClient`, supplying the bearer key resolved
     through `SecretStore`.
  3. If no row is active for that purpose, fall back to the existing
     `MockLlmClient`, which keeps the walking-skeleton flow usable
     when the operator has not yet configured a provider.
* `DbBackedSttClient` mirrors the same pattern for STT, falling back
  to the property-based `WhisperSttClient`.
* `SecretStore` is the existing abstraction from ADR 0018; the env-var
  implementation (`EnvSecretStore`) is registered with
  `@ConditionalOnMissingBean` so tests and production can swap in a
  Vault-backed implementation without code changes elsewhere.

## Rationale

* `@Primary` keeps the client surface unchanged: `LlmClient` and
  `SttProviderClient` interfaces stay; existing services keep injecting
  by interface. Only one wiring decision moves.
* Falling back to the mock/property-based client preserves the
  no-config dev experience and gives operators a smooth migration
  path: add a provider, mark a usage active, traffic switches.
* Authentication keys are never persisted in clear; the secret-store
  reference stays on the row and resolution happens at call time.

## Consequences

* The provider config UI from Iter 5 now actually changes runtime
  behaviour.
* `HttpChatCompletionClient` makes the OpenAI-compatible chat
  contract explicit: POST `endpoint_url`, body
  `{model, messages: [{role, content}, ...]}`, response
  `choices[0].message.content`. Provider-specific extensions can be
  layered on by extending the request body via the JSONB
  `parameters` column.
* `DbBackedSttClient` reuses the multipart pattern already proven in
  `WhisperSttClient`; both paths return `TranscriptionResult`.

## Alternatives

* Keep the property-based clients only — rejected, defeats the point
  of the provider tables.
* Build a separate `ProviderResolverService` that returns a strategy
  object — defensible but heavier than the inline switch for two
  client types.
