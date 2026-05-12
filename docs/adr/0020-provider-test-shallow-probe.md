# ADR 0020: Provider "test connection" is a shallow HTTP probe in phase 1

* Status: accepted
* Date: 2026-05-12

## Context

The provider configuration UI offers a "Verbindung testen" button per
LLM and STT provider (spec §7.1). The deepest possible test would send
a real prompt and a real audio sample, but that requires routing the
provider's secret through the SecretStore (ADR 0018) — work which is
deferred to phase 2.

## Decision

* `ProviderConnectionTester.probe(...)` issues a `HEAD` request with a
  5-second connect / 8-second read timeout.
* The result is interpreted as success if the upstream returns any
  HTTP status code below 500. Network errors and timeouts are
  failures.
* Latency is recorded and surfaced to the UI alongside the status.

## Rationale

* A reachability check catches the most common misconfiguration
  (typo in URL, firewall rule missing).
* It avoids the need to handle real API keys at this stage and keeps
  the failure mode predictable and quick.
* The contract is documented so phase 2 can swap in a deeper probe
  without changing UI expectations.

## Consequences

* "Test passed" does not guarantee that authentication or model
  loading work — it only proves that the host responds.
* The UI labels the result clearly ("Letzter Test") so reviewers do
  not over-interpret the green check.
* When SecretStore lands in phase 2, this method gains the secret
  and switches to a real prompt/transcription roundtrip.

## Alternatives

* Real prompt roundtrip immediately — rejected; pulls forward the
  secret-resolution work.
* No test button at all — rejected; the spec requires it and it is
  cheap to deliver shallowly.
