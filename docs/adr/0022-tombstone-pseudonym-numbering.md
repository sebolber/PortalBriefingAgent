# ADR 0022: Tombstone pseudonyms use a monotonic numeric suffix

* Status: accepted
* Date: 2026-05-13

## Context

Spec §8.3 allows tombstoned persons to be displayed as "Gelöschte
Person #N" in the UI; the underlying foreign-key relationships must
stay intact. The numbering must be deterministic and not collide.

## Decision

* `PersonTombstoneService.generatePseudonym()` counts existing
  "Gelöschte Person #" rows and produces the next number, guarded by
  an in-process `AtomicLong` so back-to-back calls within the same
  transaction can't collide.
* The pseudonym is written into `person.pseudonym`, which is what the
  UI surfaces.
* Tombstoning is idempotent: a second call on an already-tombstoned
  person is a no-op.

## Rationale

* Counting existing pseudonyms keeps the number monotonic across
  application restarts even though the in-process counter resets.
* AtomicLong prevents two concurrent administrators from picking the
  same number inside a single JVM.
* If the application ever runs in a multi-node setup, we can swap the
  AtomicLong for a database sequence without changing the public
  contract.

## Consequences

* Numbers are stable per person — once assigned they do not change.
* If the person table is restored from backup, new tombstones will
  pick up where the counter left off, which is the desired behaviour.

## Alternatives

* Use the person UUID as the pseudonym — rejected; the UI would have
  to render an opaque hex blob.
* Re-derive the suffix on every render from a row hash — rejected;
  pseudonyms would shift if the underlying row changed.
