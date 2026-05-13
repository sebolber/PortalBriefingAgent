-- Iter 5: configurable LLM / STT providers and per-author prompt templates.

CREATE TABLE llm_provider (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    endpoint_url VARCHAR(500) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    api_key_secret_ref VARCHAR(200),
    parameters JSONB,
    api_type VARCHAR(50) NOT NULL DEFAULT 'openai_compatible',
    last_tested_at TIMESTAMP,
    last_test_result VARCHAR(20),
    last_test_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT llm_provider_test_result_chk CHECK (last_test_result IS NULL OR last_test_result IN ('success', 'failed'))
);

CREATE TABLE llm_provider_usage (
    id UUID PRIMARY KEY,
    llm_provider_id UUID NOT NULL REFERENCES llm_provider(id) ON DELETE CASCADE,
    purpose VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT llm_provider_usage_purpose_chk CHECK (purpose IN (
        'audience_classification', 'summary_generation',
        'task_extraction', 'transcript_correction')),
    CONSTRAINT llm_provider_usage_unique UNIQUE (llm_provider_id, purpose)
);

CREATE UNIQUE INDEX llm_provider_usage_one_active_per_purpose
    ON llm_provider_usage (purpose) WHERE active = TRUE;

CREATE TABLE stt_provider (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    endpoint_url VARCHAR(500) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    api_key_secret_ref VARCHAR(200),
    parameters JSONB,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    last_tested_at TIMESTAMP,
    last_test_result VARCHAR(20),
    last_test_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT stt_provider_test_result_chk CHECK (last_test_result IS NULL OR last_test_result IN ('success', 'failed'))
);

CREATE UNIQUE INDEX stt_provider_one_active
    ON stt_provider (active) WHERE active = TRUE;

CREATE TABLE prompt_template (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    purpose VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    version INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_author_id UUID NOT NULL REFERENCES user_account(id),
    CONSTRAINT prompt_template_purpose_chk CHECK (purpose IN (
        'audience_classification', 'summary_generation',
        'task_extraction', 'transcript_correction'))
);

CREATE UNIQUE INDEX prompt_template_one_active_per_author_purpose
    ON prompt_template (author_id, purpose) WHERE active = TRUE;

CREATE INDEX prompt_template_author_idx ON prompt_template (author_id, purpose, version DESC);
