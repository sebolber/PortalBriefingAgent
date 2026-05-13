# ADR 0021: Retention jobs — three small Spring-Scheduling beans

* Status: accepted
* Date: 2026-05-13

## Context

Spec §6.6 defines three nightly clean-up jobs: blank old transcripts,
delete summaries beyond their per-audience horizon, and cascade-delete
authors past their grace window. Each has its own cadence (02:00,
02:15, 03:00 UTC) and failure mode.

## Decision

* One Spring `@Component` per job, each owning its `@Scheduled` cron.
* Every job depends on the shared `Clock` bean so tests can fix "today"
  to a deterministic value.
* Each job is `@Transactional` and idempotent: re-running on the same
  data is a no-op (the transcript is already null, the summary is
  already gone, the user is already deleted).
* All thresholds live in `briefingagent.retention.*` properties so an
  operator can shorten the horizon without redeploying.

## Rationale

* Splitting the jobs keeps each one focused and lets one fail without
  blocking the others.
* Tests use `Clock.fixed(...)` and verify both the success path and the
  no-op-on-second-run path.
* The retention horizons are operational knobs, not hard-coded
  business rules.

## Consequences

* Adding a new retention rule means one new bean; the scheduler
  infrastructure is reused.
* The cron strings are fixed in code; promotion to a config property
  is a follow-up if operations request it.

## Alternatives

* One mega-job — rejected; failure of one step would block the other
  two.
* Quartz / database-backed scheduler — overkill for phase 1's single
  application instance.
