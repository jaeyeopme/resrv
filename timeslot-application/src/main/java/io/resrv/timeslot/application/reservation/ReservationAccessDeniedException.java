package io.resrv.timeslot.application.reservation;

public final class ReservationAccessDeniedException extends RuntimeException {

    public ReservationAccessDeniedException() {
        super("Reservation access denied");
    }
}
