CREATE TABLE IF NOT EXISTS inspection_annotations (
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

CREATE INDEX IF NOT EXISTS inspection_annotations_session_created_idx
    ON inspection_annotations(session_id, created_at);

CREATE TABLE IF NOT EXISTS optimization_rules (
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