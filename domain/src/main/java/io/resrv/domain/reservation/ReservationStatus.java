package io.resrv.domain.reservation;

public enum ReservationStatus {
    HELD,
    CONFIRMED,
    CUSTOMER_CANCELLED,
    ADMIN_CANCELLED,
    CHECKED_IN,
    NO_SHOW,
    EXPIRED
}
