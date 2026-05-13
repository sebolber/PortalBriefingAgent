# ADR 0017: Task status history as a child table with a Clock-driven scheduler

* Status: accepted
* Date: 2026-05-12

## Context

The task lifecycle (open → in_progress → done|dropped) needs an audit
trail that survives schema migrations and feeds future analytics. The
spec also requires that the reminder scheduler be testable without
waiting for wall-clock midnight to roll around.

## Decision

* Every status transition writes a row into `task_status_history`
  (`from_status`, `to_status`, `note`, `changed_at`,
  `changed_by_author_id`). The "create" event is recorded as a
  transition `null → open`.
* `TaskService.changeStatus(...)` is the only writer; tests assert
  that no other code path mutates `task.status`.
* Time-of-day decisions are made by injecting `java.time.Clock`. The
  default bean (`Clock.systemUTC()`) is replaced with a `Clock.fixed`
  in unit tests so the scheduler is deterministic.

## Rationale

* JSONB inside `task` would cost analytics queries (e.g. average time
  open) more than it saves.
* `task_status_history` indexed by `(task_id, changed_at DESC)` makes
  per-task views trivial and analytics queries reasonable.
* Injecting `Clock` is a one-bean addition that pays for itself the
  first time a test wants to simulate "tomorrow".

## Consequences

* The reminder scheduler tests run in millisecond time using
  `Clock.fixed`.
* Reactivating a deactivated author can rebuild a coherent timeline
  by replaying the status-history rows.

## Alternatives

* In-place mutation only — rejected; loses the timeline once the row
  changes.
* JSONB array on task — rejected; harder to query and to evolve.
