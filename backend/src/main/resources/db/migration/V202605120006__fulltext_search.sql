-- Iter 7: PostgreSQL full-text search across audiences and content rows.
-- Generated tsvector columns + GIN indexes give the search service O(log n)
-- access without a separate index pipeline.

CREATE EXTENSION IF NOT EXISTS unaccent;

-- PostgreSQL only accepts IMMUTABLE expressions in GENERATED ALWAYS AS
-- STORED. The default single-argument unaccent(text) overload is marked
-- STABLE (it resolves the dictionary via search_path), so we wrap the
-- two-argument form — which names the dictionary explicitly — in an
-- IMMUTABLE wrapper. The dictionary lookup is fixed at function definition
-- time and the wrapper inherits the IMMUTABLE contract.
CREATE OR REPLACE FUNCTION immutable_unaccent(text) RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT unaccent('public.unaccent'::regdictionary, $1) $$;

ALTER TABLE person ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('german', immutable_unaccent(coalesce(full_name, ''))), 'A') ||
    setweight(to_tsvector('german', immutable_unaccent(coalesce(role, ''))), 'B') ||
    setweight(to_tsvector('german', immutable_unaccent(coalesce(company, ''))), 'B') ||
    setweight(to_tsvector('german', immutable_unaccent(coalesce(email, ''))), 'C')
) STORED;

CREATE INDEX person_search_vector_idx ON person USING GIN (search_vector);

ALTER TABLE persongroup ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('german', immutable_unaccent(coalesce(name, ''))), 'A') ||
    setweight(to_tsvector('german', immutable_unaccent(coalesce(persona_text, ''))), 'B')
) STORED;

CREATE INDEX persongroup_search_vector_idx ON persongroup USING GIN (search_vector);

ALTER TABLE topic ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('german', immutable_unaccent(coalesce(name, ''))), 'A') ||
    setweight(to_tsvector('german', immutable_unaccent(coalesce(persona_text, ''))), 'B')
) STORED;

CREATE INDEX topic_search_vector_idx ON topic USING GIN (search_vector);

ALTER TABLE ereignis ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    to_tsvector('german', immutable_unaccent(coalesce(transcript_text, '')))
) STORED;

CREATE INDEX ereignis_search_vector_idx ON ereignis USING GIN (search_vector);

ALTER TABLE summary ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    to_tsvector('german', immutable_unaccent(coalesce(summary_text, '')))
) STORED;

CREATE INDEX summary_search_vector_idx ON summary USING GIN (search_vector);
