CREATE TABLE ticketing.ticket_purchase_idempotency (
    idempotency_key VARCHAR(120) NOT NULL,
    customer_account_id UUID NOT NULL,
    ticket_event_id UUID NOT NULL REFERENCES ticketing.ticket_event(id) ON DELETE RESTRICT,
    selected_seat_ids TEXT NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    ticket_purchase_id UUID REFERENCES ticketing.ticket_purchase(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    cleanup_eligible_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (customer_account_id, idempotency_key),
    CONSTRAINT ck_ticketing_purchase_idempotency_key_not_blank
        CHECK (length(trim(idempotency_key)) > 0),
    CONSTRAINT ck_ticketing_purchase_idempotency_selected_seats_not_blank
        CHECK (length(trim(selected_seat_ids)) > 0),
    CONSTRAINT ck_ticketing_purchase_idempotency_status CHECK (
        status IN ('PENDING', 'PURCHASED', 'UNAVAILABLE_SEATS', 'VALIDATION_FAILED')
    ),
    CONSTRAINT ck_ticketing_purchase_idempotency_purchase_state CHECK (
        (status = 'PURCHASED' AND ticket_purchase_id IS NOT NULL AND completed_at IS NOT NULL)
        OR (status <> 'PURCHASED' AND ticket_purchase_id IS NULL)
        OR status = 'PENDING'
    ),
    CONSTRAINT ck_ticketing_purchase_idempotency_expiry CHECK (
        created_at < expires_at AND expires_at < cleanup_eligible_at
    )
);

CREATE INDEX idx_ticketing_purchase_idempotency_event
    ON ticketing.ticket_purchase_idempotency(ticket_event_id);

CREATE INDEX idx_ticketing_purchase_idempotency_cleanup
    ON ticketing.ticket_purchase_idempotency(cleanup_eligible_at);
