package io.resrv.timeslot.application.reservation.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.ReservationId;
import java.util.Objects;

public record CustomerReservationDetailQuery(AccountId accountId, ReservationId reservationId) {

    public CustomerReservationDetailQuery {
        Objects.requireNonNull(accountId, "Account id must not be null");
        Objects.requireNonNull(reservationId, "Reservation id must not be null");
    }
}
