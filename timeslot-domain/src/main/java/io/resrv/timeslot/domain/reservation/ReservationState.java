package io.resrv.timeslot.domain.reservation;

public enum ReservationState {
    HELD,
    EXPIRED,
    RELEASED,
    CONFIRMED,
    CUSTOMER_CANCELLED,
    BUSINESS_CANCELLED,
    CHECKED_IN,
    NO_SHOW
}
