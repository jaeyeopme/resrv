CREATE TABLE customer (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_customer_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT ck_customer_email_not_blank CHECK (length(trim(email)) > 0),
    CONSTRAINT ck_customer_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE INDEX idx_customer_tenant_id ON customer(tenant_id);
