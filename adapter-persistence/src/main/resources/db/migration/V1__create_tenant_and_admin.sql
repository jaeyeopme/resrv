CREATE TABLE tenant (
    id                  UUID            PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    slug                VARCHAR(63)     NOT NULL,
    timezone            VARCHAR(64)     NOT NULL,
    slot_duration       INT             NOT NULL,
    hold_ttl            INT             NOT NULL,
    cancellation_window INT             NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,

    CONSTRAINT uq_tenant_slug UNIQUE (slug),
    CONSTRAINT ck_tenant_slot_duration CHECK (slot_duration >= 30 AND slot_duration <= 480 AND slot_duration % 30 = 0),
    CONSTRAINT ck_tenant_hold_ttl CHECK (hold_ttl >= 5 AND hold_ttl <= 30),
    CONSTRAINT ck_tenant_cancellation_window CHECK (cancellation_window >= 0)
);

CREATE TABLE admin (
    id              UUID            PRIMARY KEY,
    tenant_id       UUID            NOT NULL REFERENCES tenant(id),
    email           VARCHAR(255)    NOT NULL,
    hashed_password VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,

    CONSTRAINT uq_admin_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_admin_tenant_id ON admin(tenant_id);
