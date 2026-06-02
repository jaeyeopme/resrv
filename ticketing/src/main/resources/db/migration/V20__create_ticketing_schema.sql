CREATE SCHEMA IF NOT EXISTS ticketing;

CREATE TABLE ticketing.ticket_event (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    event_start_at TIMESTAMPTZ NOT NULL,
    event_end_at TIMESTAMPTZ NOT NULL,
    event_timezone VARCHAR(64) NOT NULL,
    sale_start_at TIMESTAMPTZ NOT NULL,
    sale_end_at TIMESTAMPTZ NOT NULL,
    sale_timezone VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ticketing_event_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT ck_ticketing_event_occurrence_range CHECK (event_start_at < event_end_at),
    CONSTRAINT ck_ticketing_event_sale_range CHECK (sale_start_at < sale_end_at),
    CONSTRAINT ck_ticketing_event_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_ticketing_event_business_status
    ON ticketing.ticket_event(business_id, status);

CREATE TABLE ticketing.ticket_inventory (
    id UUID PRIMARY KEY,
    ticket_event_id UUID NOT NULL REFERENCES ticketing.ticket_event(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ticketing_inventory_event UNIQUE (ticket_event_id)
);

CREATE TABLE ticketing.ticket_inventory_tier (
    id UUID PRIMARY KEY,
    ticket_inventory_id UUID NOT NULL REFERENCES ticketing.ticket_inventory(id) ON DELETE CASCADE,
    display_name VARCHAR(200) NOT NULL,
    total INT NOT NULL,
    reserved INT NOT NULL,
    confirmed INT NOT NULL,
    soft_reserved INT NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT ck_ticketing_tier_display_name_not_blank CHECK (length(trim(display_name)) > 0),
    CONSTRAINT ck_ticketing_tier_non_negative CHECK (
        total >= 0
        AND reserved >= 0
        AND confirmed >= 0
        AND soft_reserved >= 0
    ),
    CONSTRAINT ck_ticketing_tier_capacity CHECK (
        reserved + confirmed + soft_reserved <= total
    ),
    CONSTRAINT uq_ticketing_tier_order UNIQUE (ticket_inventory_id, sort_order)
);

CREATE INDEX idx_ticketing_inventory_event
    ON ticketing.ticket_inventory(ticket_event_id);
