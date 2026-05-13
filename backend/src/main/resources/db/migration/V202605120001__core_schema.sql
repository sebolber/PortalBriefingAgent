-- Briefing Agent core schema (Iter 0 walking skeleton).
-- Tables in this migration follow Section 5 of the phase-1 spec.
-- Extensions for full-text search arrive in a later iteration.

CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    entra_object_id VARCHAR(36),
    entra_upn VARCHAR(255),
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    deactivated_at TIMESTAMP,
    deletion_scheduled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT user_account_status_chk CHECK (status IN ('active', 'inactive'))
);

CREATE TABLE person (
    id UUID PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(200),
    company VARCHAR(200),
    source VARCHAR(20) NOT NULL DEFAULT 'manual',
    deleted_at TIMESTAMP,
    pseudonym VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT person_source_chk CHECK (source IN ('manual', 'entra'))
);

CREATE TABLE person_persona (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    person_id UUID NOT NULL REFERENCES person(id),
    persona_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT person_persona_unique UNIQUE (author_id, person_id)
);

CREATE TABLE persongroup (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    name VARCHAR(200) NOT NULL,
    persona_text TEXT NOT NULL,
    summary_retention_months INT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT persongroup_retention_positive CHECK (summary_retention_months IS NULL OR summary_retention_months > 0)
);

CREATE TABLE persongroup_member (
    persongroup_id UUID NOT NULL REFERENCES persongroup(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES person(id),
    PRIMARY KEY (persongroup_id, person_id)
);

CREATE TABLE topic (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    name VARCHAR(200) NOT NULL,
    persona_text TEXT NOT NULL,
    summary_retention_months INT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT topic_retention_positive CHECK (summary_retention_months IS NULL OR summary_retention_months > 0)
);

CREATE TABLE topic_member (
    topic_id UUID NOT NULL REFERENCES topic(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES person(id),
    PRIMARY KEY (topic_id, person_id)
);

CREATE TABLE ereignis (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    source_type VARCHAR(20) NOT NULL,
    transcript_text TEXT,
    transcript_source VARCHAR(20),
    language VARCHAR(10),
    duration_seconds INT,
    character_count INT,
    truncated_at_limit BOOLEAN NOT NULL DEFAULT FALSE,
    review_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    transcript_retention_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ereignis_source_type_chk CHECK (source_type IN ('audio', 'text')),
    CONSTRAINT ereignis_transcript_source_chk CHECK (transcript_source IS NULL OR transcript_source IN ('whisper', 'manual')),
    CONSTRAINT ereignis_review_status_chk CHECK (review_status IN ('pending', 'reviewed', 'released'))
);

CREATE INDEX ereignis_author_created_idx ON ereignis (author_id, created_at DESC);

CREATE TABLE summary (
    id UUID PRIMARY KEY,
    ereignis_id UUID NOT NULL REFERENCES ereignis(id) ON DELETE CASCADE,
    audience_type VARCHAR(20) NOT NULL,
    audience_person_id UUID REFERENCES person(id),
    audience_persongroup_id UUID REFERENCES persongroup(id),
    audience_topic_id UUID REFERENCES topic(id),
    summary_text TEXT NOT NULL,
    classification_confidence VARCHAR(10),
    classification_reasoning TEXT,
    edit_state VARCHAR(20) NOT NULL DEFAULT 'ai_generated',
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT summary_audience_type_chk CHECK (audience_type IN ('person', 'persongroup', 'topic')),
    CONSTRAINT summary_confidence_chk CHECK (classification_confidence IS NULL OR classification_confidence IN ('low', 'medium', 'high')),
    CONSTRAINT summary_edit_state_chk CHECK (edit_state IN ('ai_generated', 'manually_edited', 'regenerated')),
    CONSTRAINT summary_one_audience_target CHECK (
        (CASE WHEN audience_person_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN audience_persongroup_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN audience_topic_id IS NOT NULL THEN 1 ELSE 0 END) = 1
    )
);

CREATE INDEX summary_ereignis_idx ON summary (ereignis_id);
