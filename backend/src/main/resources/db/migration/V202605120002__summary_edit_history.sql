-- Iter 3: append-only edit history for summaries. Each entry captures
-- the change that produced the new summary text so a reviewer can
-- reconstruct the timeline. JSONB is PostgreSQL-specific, which is
-- fine for production; the H2-backed test profile does not load this
-- migration because Hibernate validates against the generated schema
-- there (see EreignisService unit tests run with mocks).

ALTER TABLE summary
    ADD COLUMN edit_history JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE summary
    ADD COLUMN regeneration_feedback TEXT;

CREATE INDEX summary_edit_history_gin ON summary USING GIN (edit_history);
