package io.resrv.timeslot.domain.reservation;

public final class ReservationHoldExpiredException extends RuntimeException {

    public ReservationHoldExpiredException(final String message) {
        super(message);
    }
}
