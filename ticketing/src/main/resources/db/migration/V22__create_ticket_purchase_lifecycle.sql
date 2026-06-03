CREATE TABLE ticketing.ticket_seat (
    id UUID PRIMARY KEY,
    ticket_event_id UUID NOT NULL REFERENCES ticketing.ticket_event(id) ON DELETE CASCADE,
    display_label VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    purchased_at TIMESTAMPTZ,
    purchase_id UUID,
    CONSTRAINT ck_ticketing_seat_display_label_not_blank CHECK (length(trim(display_label)) > 0),
    CONSTRAINT ck_ticketing_seat_status CHECK (status IN ('AVAILABLE', 'PURCHASED')),
    CONSTRAINT ck_ticketing_seat_purchase_state CHECK (
        (status = 'AVAILABLE' AND purchased_at IS NULL AND purchase_id IS NULL)
        OR (status = 'PURCHASED' AND purchased_at IS NOT NULL AND purchase_id IS NOT NULL)
    )
);

CREATE INDEX idx_ticketing_seat_event_status
    ON ticketing.ticket_seat(ticket_event_id, status);

CREATE TABLE ticketing.ticket_purchase (
    id UUID PRIMARY KEY,
    ticket_event_id UUID NOT NULL REFERENCES ticketing.ticket_event(id) ON DELETE RESTRICT,
    customer_account_id UUID NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ticketing_purchase_customer
    ON ticketing.ticket_purchase(customer_account_id, confirmed_at DESC);

CREATE INDEX idx_ticketing_purchase_event
    ON ticketing.ticket_purchase(ticket_event_id, confirmed_at DESC);

CREATE TABLE ticketing.ticket_purchase_seat (
    ticket_purchase_id UUID NOT NULL REFERENCES ticketing.ticket_purchase(id) ON DELETE CASCADE,
    ticket_seat_id UUID NOT NULL REFERENCES ticketing.ticket_seat(id) ON DELETE RESTRICT,
    PRIMARY KEY (ticket_purchase_id, ticket_seat_id),
    CONSTRAINT uq_ticketing_purchase_seat UNIQUE (ticket_seat_id)
);

ALTER TABLE ticketing.ticket_seat
    ADD CONSTRAINT fk_ticketing_seat_purchase
    FOREIGN KEY (purchase_id)
    REFERENCES ticketing.ticket_purchase(id)
    ON DELETE RESTRICT;
