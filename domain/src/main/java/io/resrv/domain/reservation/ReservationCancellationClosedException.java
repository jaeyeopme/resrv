package io.resrv.domain.reservation;

public final class ReservationCancellationClosedException extends RuntimeException {

    public ReservationCancellationClosedException(final ReservationId reservationId) {
        super("Reservation '%s' can no longer be cancelled".formatted(reservationId.value()));
    }
}
