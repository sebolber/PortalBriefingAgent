# ADR 0011: Person, PersonGroup and Topic stay as three separate entities

* Status: accepted
* Date: 2026-05-12

## Context

The classifier and the summary generator both have to address one of three
audience flavours: an individual person, a fixed group of people, or a
subject area. Spec §4.2 asks the question explicitly.

## Decision

Keep three concrete tables (`person`, `persongroup`, `topic`) plus their
join tables, instead of folding everything into a single `audience`
table with a discriminator column.

## Rationale

* Each flavour has its own integrity rules: a person can be tombstoned;
  a group has members; a topic has responsible persons. Trying to
  merge them into one row would force every column to be nullable.
* The summary table already carries three nullable foreign keys plus a
  CHECK constraint that exactly one of them is set — this exact-one
  semantics maps cleanly to the three tables.
* The classifier prompt enumerates audiences with type tags
  (`person|persongroup|topic`); the LLM response references the type
  explicitly, and our parser cross-checks it against the entity it
  resolves. The check would be lost in a uniform table.

## Consequences

* `AudienceQueryService` aggregates the three tables into a uniform
  `AudienceRef` for downstream consumers. The aggregation cost is
  acceptable for phase 1 (small per-author counts) and avoids the
  modelling debt of a single table.
* `SummaryGenerationService` switches on the type once when materialising
  a `Summary` row; the rest of the pipeline stays type-agnostic.

## Alternatives

* Single `audience` table with `audience_type` + nullable foreign keys
  back to person/persongroup/topic — rejected; it doubles the join
  surface without removing the underlying schema split.
