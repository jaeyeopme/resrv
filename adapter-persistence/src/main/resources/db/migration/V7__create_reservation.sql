CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reservation (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    resource_id UUID NOT NULL REFERENCES resource(id),
    customer_id UUID NOT NULL REFERENCES customer(id),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    hold_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT ck_reservation_time_range CHECK (start_at < end_at),
    CONSTRAINT ck_reservation_status CHECK (
        status IN ('HELD','CONFIRMED','CUSTOMER_CANCELLED','ADMIN_CANCELLED','CHECKED_IN','NO_SHOW','EXPIRED')
    ),
    CONSTRAINT ck_reservation_hold_expiry CHECK (
        (status = 'HELD' AND hold_expires_at IS NOT NULL) OR status <> 'HELD'
    )
);

ALTER TABLE reservation
    ADD CONSTRAINT ex_reservation_no_active_overlap
    EXCLUDE USING gist (
        tenant_id WITH =,
        resource_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status IN ('HELD', 'CONFIRMED', 'CHECKED_IN'));

CREATE INDEX idx_reservation_customer ON reservation(tenant_id, customer_id, start_at);
CREATE INDEX idx_reservation_resource_window ON reservation(tenant_id, resource_id, start_at, end_at);
CREATE INDEX idx_reservation_hold_expiry ON reservation(status, hold_expires_at);
