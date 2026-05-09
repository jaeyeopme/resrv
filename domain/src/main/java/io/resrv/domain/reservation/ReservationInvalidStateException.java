package io.resrv.domain.reservation;

public final class ReservationInvalidStateException extends RuntimeException {

    public ReservationInvalidStateException(
            final ReservationId reservationId,
            final ReservationStatus status,
            final String action) {
        super(
                "Reservation '%s' in status '%s' cannot be %s"
                        .formatted(reservationId.value(), status.name(), action));
    }
}
