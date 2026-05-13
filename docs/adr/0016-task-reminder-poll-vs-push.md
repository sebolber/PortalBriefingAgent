# ADR 0016: Task reminders surfaced via polling, not Web-Push

* Status: accepted
* Date: 2026-05-12

## Context

Spec §6.6 calls for "Push-Notifications" when a task's due date is
near. Browser Web-Push is the obvious fit but requires a service-worker
subscription flow, VAPID keys and an external push gateway. For phase
1 the audience is 2–10 internal authors who already keep the dashboard
open during work hours.

## Decision

* The backend persists a {@code task_reminder} row per (task,
  reminder_type, calendar day) when the daily scheduler decides a
  reminder is due.
* The frontend polls {@code GET /api/notifications} (≤ once per
  minute) and renders the unread reminders as a badge.
* No service worker, no VAPID, no push gateway in phase 1.

## Rationale

* Polling is simple to operate, fully under our control, and
  recoverable after a missed window.
* Phase 2 can layer Web-Push (or Teams notifications) on top of the
  same `task_reminder` rows without changing the schema.
* The unique index on (task_id, reminder_type, reminded_on) keeps the
  reminder log idempotent across job restarts.

## Consequences

* Latency for a reminder appearing is bounded by the poll interval.
* Service worker scaffolding is deferred to phase 2.

## Alternatives

* Web-Push immediately — rejected for phase-1 simplicity.
* Server-sent events / WebSockets — comparable benefit to polling for
  this audience, more moving parts.
