CREATE SCHEMA IF NOT EXISTS timeslot;

CREATE TABLE timeslot.business_booking_settings (
    business_id UUID PRIMARY KEY,
    slot_duration_minutes INT NOT NULL,
    hold_ttl_minutes INT NOT NULL,
    cancellation_window_minutes INT NOT NULL,
    max_advance_booking_days INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_timeslot_slot_duration CHECK (
        slot_duration_minutes >= 5
        AND slot_duration_minutes <= 480
        AND slot_duration_minutes % 5 = 0
    ),
    CONSTRAINT ck_timeslot_hold_ttl CHECK (hold_ttl_minutes >= 1 AND hold_ttl_minutes <= 30),
    CONSTRAINT ck_timeslot_cancel_window CHECK (
        cancellation_window_minutes >= 0
        AND cancellation_window_minutes <= 10080
    ),
    CONSTRAINT ck_timeslot_max_advance CHECK (
        max_advance_booking_days >= 1
        AND max_advance_booking_days <= 365
    )
);

CREATE TABLE timeslot.resource (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    slug VARCHAR(63) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    slot_duration_minutes INT,
    hold_ttl_minutes INT,
    cancellation_window_minutes INT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_timeslot_resource_business_slug UNIQUE (business_id, slug),
    CONSTRAINT ck_timeslot_resource_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_timeslot_resource_slug_not_blank CHECK (length(trim(slug)) > 0),
    CONSTRAINT ck_timeslot_resource_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_timeslot_resource_slot_override CHECK (
        slot_duration_minutes IS NULL
        OR (
            slot_duration_minutes >= 5
            AND slot_duration_minutes <= 480
            AND slot_duration_minutes % 5 = 0
        )
    ),
    CONSTRAINT ck_timeslot_resource_hold_override CHECK (
        hold_ttl_minutes IS NULL OR (hold_ttl_minutes >= 1 AND hold_ttl_minutes <= 30)
    ),
    CONSTRAINT ck_timeslot_resource_cancel_override CHECK (
        cancellation_window_minutes IS NULL
        OR (cancellation_window_minutes >= 0 AND cancellation_window_minutes <= 10080)
    )
);

CREATE INDEX idx_timeslot_resource_business_status
    ON timeslot.resource(business_id, status);

CREATE TABLE timeslot.resource_weekly_schedule (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_timeslot_resource_weekly_schedule UNIQUE (business_id, resource_id, day_of_week),
    CONSTRAINT ck_timeslot_weekly_day CHECK (day_of_week BETWEEN 1 AND 7)
);

CREATE TABLE timeslot.resource_weekly_schedule_window (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL REFERENCES timeslot.resource_weekly_schedule(id) ON DELETE CASCADE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT ck_timeslot_weekly_window_range CHECK (start_time < end_time),
    CONSTRAINT uq_timeslot_weekly_window_order UNIQUE (schedule_id, sort_order)
);

CREATE TABLE timeslot.resource_date_schedule_override (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_timeslot_resource_date_override UNIQUE (business_id, resource_id, date)
);

CREATE TABLE timeslot.resource_date_schedule_override_window (
    id UUID PRIMARY KEY,
    override_id UUID NOT NULL REFERENCES timeslot.resource_date_schedule_override(id) ON DELETE CASCADE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT ck_timeslot_date_window_range CHECK (start_time < end_time),
    CONSTRAINT uq_timeslot_date_window_order UNIQUE (override_id, sort_order)
);

CREATE TABLE timeslot.reservation (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    customer_account_id UUID NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    hold_expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancelled_by VARCHAR(32),
    checked_in_at TIMESTAMPTZ,
    no_show_at TIMESTAMPTZ,
    CONSTRAINT ck_timeslot_reservation_time_range CHECK (start_at < end_at),
    CONSTRAINT ck_timeslot_reservation_cancel_actor CHECK (
        (cancelled_at IS NULL AND cancelled_by IS NULL)
        OR (cancelled_at IS NOT NULL AND cancelled_by IN ('CUSTOMER', 'BUSINESS'))
    ),
    CONSTRAINT ck_timeslot_reservation_release_terminal CHECK (
        released_at IS NULL
        OR (
            confirmed_at IS NULL
            AND cancelled_at IS NULL
            AND checked_in_at IS NULL
            AND no_show_at IS NULL
        )
    ),
    CONSTRAINT ck_timeslot_reservation_confirmed_terminal CHECK (
        (checked_in_at IS NULL OR confirmed_at IS NOT NULL)
        AND (no_show_at IS NULL OR confirmed_at IS NOT NULL)
    ),
    CONSTRAINT ck_timeslot_reservation_single_terminal CHECK (
        (CASE WHEN cancelled_at IS NULL THEN 0 ELSE 1 END)
        + (CASE WHEN checked_in_at IS NULL THEN 0 ELSE 1 END)
        + (CASE WHEN no_show_at IS NULL THEN 0 ELSE 1 END)
        <= 1
    )
);

CREATE INDEX idx_timeslot_reservation_customer
    ON timeslot.reservation(business_id, customer_account_id, start_at);

CREATE INDEX idx_timeslot_reservation_resource_window
    ON timeslot.reservation(business_id, resource_id, start_at, end_at);

CREATE INDEX idx_timeslot_reservation_active_blocker
    ON timeslot.reservation(business_id, resource_id, start_at, end_at, hold_expires_at);
