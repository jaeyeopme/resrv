package io.resrv.timeslot.domain.reservation;

public final class ReservationInvalidStateException extends RuntimeException {

    public ReservationInvalidStateException(final String message) {
        super(message);
    }
}
