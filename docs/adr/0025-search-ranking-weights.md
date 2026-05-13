# ADR 0025: Search ranking weights — A for names, B for personas/roles, C for emails

* Status: accepted
* Date: 2026-05-13

## Context

Two factors decide whether the search felt good in user testing: that
the right hit appears at the top, and that the snippet makes the hit
recognisable at a glance. PostgreSQL's `setweight` mechanism gives us
an out-of-the-box way to express the priorities.

## Decision

| Field | Weight | Reason |
|-------|--------|--------|
| `person.full_name`, `persongroup.name`, `topic.name` | A (highest) | The literal name a user types is the most obvious signal. |
| `person.role`, `person.company`, persona texts | B | Useful but secondary; the user typed a person, not their job title. |
| `person.email` | C | Almost never the search anchor, but a useful disambiguator. |
| `ereignis.transcript_text`, `summary.summary_text` | default | Bulk content; relevance is largely lexical. |

Ranking uses `ts_rank_cd` so document length normalisation does not
unfairly penalise long persona texts.

## Rationale

* `setweight` is encoded inside the GENERATED column, so every search
  benefits without code-side weighting logic.
* `ts_rank_cd` rewards matches in shorter fields (`name` is short,
  transcripts are long), which aligns with intuition.

## Consequences

* Tuning the weights is a schema change, not a code change. Migrations
  that adjust weights require a `REINDEX` of the GIN index.
* When a richer ranking model (vector embeddings, learning to rank) is
  considered in phase 2, the fallback path still works.

## Alternatives

* Apply weights in SQL per query — possible but more code and one
  more opportunity for inconsistency.
* Leave everything at default weight — search would return long
  persona texts before the name they describe.
