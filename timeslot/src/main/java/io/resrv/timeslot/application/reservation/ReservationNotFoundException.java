package io.resrv.timeslot.application.reservation;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;

public final class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(final ReservationId reservationId) {
        super("Reservation not found");
    }

    public ReservationNotFoundException(
            final BusinessId businessId, final ReservationId reservationId) {
        super(
                "Reservation not found for business "
                        + businessId.value()
                        + ": "
                        + reservationId.value());
    }
}
