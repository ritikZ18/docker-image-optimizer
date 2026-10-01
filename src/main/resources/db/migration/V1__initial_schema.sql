CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL
);

CREATE TABLE optimization_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID NOT NULL REFERENCES repositories(id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE inspection_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID NOT NULL REFERENCES repositories(id),
    session_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    file_count INTEGER NOT NULL,
    line_count BIGINT NOT NULL,
    dockerfile_count INTEGER NOT NULL,
    evidence_json TEXT NOT NULL
);

CREATE INDEX inspection_sessions_repository_created_idx
    ON inspection_sessions(repository_id, created_at DESC);

CREATE TABLE inspection_annotations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES inspection_sessions(id),
    category VARCHAR(255) NOT NULL,
    severity VARCHAR(64) NOT NULL,
    file_path VARCHAR(1000),
    line_number INTEGER,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    detail TEXT,
    evidence_json TEXT
);

CREATE INDEX inspection_annotations_session_created_idx
    ON inspection_annotations(session_id, created_at);

CREATE TABLE optimization_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_code VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(255),
    language VARCHAR(255),
    applicability VARCHAR(1000),
    source_url VARCHAR(1000),
    documentation_version VARCHAR(255),
    last_verified_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL,
    description TEXT NOT NULL
);