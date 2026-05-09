CREATE TABLE resource_weekly_availability (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    resource_id UUID NOT NULL REFERENCES resource(id),
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_resource_weekly_availability UNIQUE (tenant_id, resource_id, day_of_week),
    CONSTRAINT ck_weekly_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_weekly_time_range CHECK (start_time < end_time)
);

CREATE INDEX idx_resource_weekly_availability_resource
    ON resource_weekly_availability(tenant_id, resource_id);

CREATE TABLE resource_availability_exception (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    resource_id UUID NOT NULL REFERENCES resource(id),
    date DATE NOT NULL,
    closed BOOLEAN NOT NULL,
    start_time TIME,
    end_time TIME,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_resource_availability_exception UNIQUE (tenant_id, resource_id, date),
    CONSTRAINT ck_exception_time_contract CHECK (
        (closed = true AND start_time IS NULL AND end_time IS NULL)
        OR (closed = false AND start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
    )
);

CREATE INDEX idx_resource_availability_exception_resource
    ON resource_availability_exception(tenant_id, resource_id);
