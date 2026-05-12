# ADR 0003: BCrypt cost 12 as the default password hash strength

* Status: accepted
* Date: 2026-05-12

## Context

Phase 1 uses local authentication (username + password). The
implementation rules require that passwords are stored hashed with a
strong, salted scheme. BCrypt is the framework default.

## Decision

* Default cost factor: **12**.
* Minimum cost (validated by `@Min(10)` on
  `briefingagent.security.bcrypt-strength`): **10**.

## Rationale

* OWASP recommends cost 12 in 2024 for interactive logins.
* On commodity hardware that yields a ~250 ms hash, which is acceptable
  for an internal app of 2–10 users.
* The validation floor of 10 prevents accidentally weakening the
  configuration via misconfigured environment variables. A
  configuration error is louder than a silent downgrade.

## Consequences

* Login latency: ~250 ms per request.
* Tests that exercise the real `BCryptPasswordEncoder` cope with this
  cost. Slice tests that do not hash anything are unaffected because the
  authentication manager is mocked.

## Alternatives

* Argon2id — stronger but pulls in another dependency. Reserved as a
  Phase-2 upgrade if security review demands it.
* Lower cost in dev / tests — rejected; we keep one cost across profiles
  for behavioural fidelity.
