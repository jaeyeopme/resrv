package io.resrv.domain.reservation;

public final class ReservationHoldExpiredException extends RuntimeException {

    public ReservationHoldExpiredException(final ReservationId reservationId) {
        super("Reservation hold '%s' has expired".formatted(reservationId.value()));
    }
}
