CREATE TABLE resource (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    slug VARCHAR(63) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_resource_tenant_slug UNIQUE (tenant_id, slug),
    CONSTRAINT chk_resource_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_resource_slug_not_blank CHECK (length(trim(slug)) > 0),
    CONSTRAINT chk_resource_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE INDEX idx_resource_tenant_status ON resource (tenant_id, status);
