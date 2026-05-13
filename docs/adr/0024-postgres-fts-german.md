# ADR 0024: Full-text search via PostgreSQL `to_tsvector('german', …)` + GIN

* Status: accepted
* Date: 2026-05-13

## Context

Spec §6.4 calls for global search across persons, person groups,
topics, ereignisse and summaries with the German dictionary, accent
folding and a relevance ranking. Phase 1 cannot afford a separate
search service.

## Decision

* Each searchable table gains a `search_vector tsvector GENERATED
  ALWAYS AS (...) STORED` column built from the relevant fields with
  weighted contributions (`setweight`).
* `unaccent` from the `unaccent` extension folds umlauts so
  `Müller` and `Mueller` match.
* A GIN index on each `search_vector` answers searches in O(log n).
* The `SearchService` issues five small native queries (one per
  bucket), all parameterised through `plainto_tsquery('german', ...)`
  so the user query can never escape parameter binding.
* Results across buckets are merged and sorted by `ts_rank_cd`
  before being returned to the client.

## Rationale

* PostgreSQL's German dictionary is good enough for phase 1 and avoids
  introducing Elasticsearch/OpenSearch as a separate service.
* `plainto_tsquery` rejects malicious operators; the API takes plain
  text and converts to a query inside the database.
* `setweight` lets us prioritise the obvious fields (name) over the
  long ones (persona text) without a hand-rolled ranking function.

## Consequences

* The migration requires the `unaccent` extension; production
  installations need this extension allow-listed (it ships with
  contrib).
* Adding a new searchable column means widening one `tsvector`
  expression — the index is rebuilt automatically.

## Alternatives

* Lucene/Elasticsearch — too much operational overhead for 2–10
  authors.
* `ILIKE '%q%'` — works but bypasses tokenisation and ranking entirely.
