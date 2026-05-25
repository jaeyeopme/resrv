CREATE SCHEMA IF NOT EXISTS platform;

CREATE TABLE platform.account (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_platform_account_email UNIQUE (email),
    CONSTRAINT ck_platform_account_email_not_blank CHECK (length(trim(email)) > 0),
    CONSTRAINT ck_platform_account_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_platform_account_hashed_password_not_blank CHECK (length(trim(hashed_password)) > 0),
    CONSTRAINT ck_platform_account_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE platform.business (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(63) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_platform_business_slug UNIQUE (slug),
    CONSTRAINT ck_platform_business_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_platform_business_slug_not_blank CHECK (length(trim(slug)) > 0),
    CONSTRAINT ck_platform_business_timezone_not_blank CHECK (length(trim(timezone)) > 0),
    CONSTRAINT ck_platform_business_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE platform.business_membership (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    business_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_platform_business_membership_account_business UNIQUE (account_id, business_id),
    CONSTRAINT ck_platform_business_membership_role CHECK (role IN ('OWNER', 'STAFF'))
);

CREATE INDEX idx_platform_business_membership_account
    ON platform.business_membership(account_id, active);

CREATE INDEX idx_platform_business_membership_business
    ON platform.business_membership(business_id, active);
