package io.resrv.timeslot.application.reservation.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.reservation.ReservationState;
import java.time.LocalDate;
import java.util.Objects;

public record ListBusinessReservationsQuery(
        BusinessId businessId,
        AccountId accountId,
        LocalDate date,
        ResourceId resourceId,
        AccountId customerAccountId,
        ReservationState state) {

    public ListBusinessReservationsQuery {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(accountId, "Account id must not be null");
        Objects.requireNonNull(date, "Date must not be null");
    }
}
