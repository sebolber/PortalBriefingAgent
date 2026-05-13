-- Iter 7: PostgreSQL full-text search across audiences and content rows.
-- Generated tsvector columns + GIN indexes give the search service O(log n)
-- access without a separate index pipeline.

CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE person ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('german', unaccent(coalesce(full_name, ''))), 'A') ||
    setweight(to_tsvector('german', unaccent(coalesce(role, ''))), 'B') ||
    setweight(to_tsvector('german', unaccent(coalesce(company, ''))), 'B') ||
    setweight(to_tsvector('german', unaccent(coalesce(email, ''))), 'C')
) STORED;

CREATE INDEX person_search_vector_idx ON person USING GIN (search_vector);

ALTER TABLE persongroup ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('german', unaccent(coalesce(name, ''))), 'A') ||
    setweight(to_tsvector('german', unaccent(coalesce(persona_text, ''))), 'B')
) STORED;

CREATE INDEX persongroup_search_vector_idx ON persongroup USING GIN (search_vector);

ALTER TABLE topic ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('german', unaccent(coalesce(name, ''))), 'A') ||
    setweight(to_tsvector('german', unaccent(coalesce(persona_text, ''))), 'B')
) STORED;

CREATE INDEX topic_search_vector_idx ON topic USING GIN (search_vector);

ALTER TABLE ereignis ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    to_tsvector('german', unaccent(coalesce(transcript_text, '')))
) STORED;

CREATE INDEX ereignis_search_vector_idx ON ereignis USING GIN (search_vector);

ALTER TABLE summary ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    to_tsvector('german', unaccent(coalesce(summary_text, '')))
) STORED;

CREATE INDEX summary_search_vector_idx ON summary USING GIN (search_vector);
