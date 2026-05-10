CREATE TABLE revoked_token (
    jti TEXT PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_revoked_token_jti_not_blank CHECK (length(trim(jti)) > 0)
);

CREATE INDEX idx_revoked_token_expires_at ON revoked_token(expires_at);
