CREATE TABLE platform.sign_in_attempt (
    id UUID PRIMARY KEY,
    account_id UUID,
    email_hash VARCHAR(128) NOT NULL,
    caller_fingerprint VARCHAR(255) NOT NULL,
    outcome VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_platform_sign_in_attempt_account
        FOREIGN KEY (account_id) REFERENCES platform.account(id),
    CONSTRAINT ck_platform_sign_in_attempt_email_hash_not_blank CHECK (length(trim(email_hash)) > 0),
    CONSTRAINT ck_platform_sign_in_attempt_caller_fingerprint_not_blank CHECK (length(trim(caller_fingerprint)) > 0),
    CONSTRAINT ck_platform_sign_in_attempt_outcome CHECK (
        outcome IN ('FAILED_UNKNOWN_ACCOUNT', 'FAILED_BAD_PASSWORD', 'FAILED_REQUIRES_RESET', 'SUCCESS')
    )
);

CREATE INDEX idx_platform_sign_in_attempt_account_occurred
    ON platform.sign_in_attempt(account_id, occurred_at DESC);

CREATE INDEX idx_platform_sign_in_attempt_email_hash_occurred
    ON platform.sign_in_attempt(email_hash, occurred_at DESC);

CREATE TABLE platform.account_sign_in_protection (
    account_id UUID PRIMARY KEY,
    failed_password_attempts INTEGER NOT NULL,
    password_reset_required BOOLEAN NOT NULL,
    password_reset_required_at TIMESTAMPTZ,
    last_failed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_platform_account_sign_in_protection_account
        FOREIGN KEY (account_id) REFERENCES platform.account(id),
    CONSTRAINT ck_platform_account_sign_in_protection_attempts_non_negative
        CHECK (failed_password_attempts >= 0),
    CONSTRAINT ck_platform_account_sign_in_protection_required_at CHECK (
        (password_reset_required = false AND password_reset_required_at IS NULL)
        OR (password_reset_required = true AND password_reset_required_at IS NOT NULL)
    )
);

CREATE TABLE platform.password_reset_challenge (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    token_digest VARCHAR(128) NOT NULL,
    reason VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    replaced_at TIMESTAMPTZ,
    CONSTRAINT fk_platform_password_reset_challenge_account
        FOREIGN KEY (account_id) REFERENCES platform.account(id),
    CONSTRAINT uq_platform_password_reset_challenge_token_digest UNIQUE (token_digest),
    CONSTRAINT ck_platform_password_reset_challenge_token_digest_not_blank CHECK (length(trim(token_digest)) > 0),
    CONSTRAINT ck_platform_password_reset_challenge_reason CHECK (reason IN ('FAILED_PASSWORD_ATTEMPTS')),
    CONSTRAINT ck_platform_password_reset_challenge_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_platform_password_reset_challenge_account_active
    ON platform.password_reset_challenge(account_id, created_at DESC)
    WHERE used_at IS NULL AND replaced_at IS NULL;
