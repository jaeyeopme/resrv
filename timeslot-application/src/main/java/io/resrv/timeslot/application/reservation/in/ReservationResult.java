package io.resrv.timeslot.application.reservation.in;

import io.resrv.timeslot.domain.reservation.Reservation;
import io.resrv.timeslot.domain.reservation.ReservationState;
import java.time.Instant;
import java.util.UUID;

public record ReservationResult(
        UUID id,
        UUID businessId,
        UUID resourceId,
        UUID customerAccountId,
        Instant startAt,
        Instant endAt,
        ReservationState state,
        Instant holdExpiresAt,
        Instant createdAt,
        Instant updatedAt) {

    public static ReservationResult from(final Reservation reservation, final Instant now) {
        return new ReservationResult(
                reservation.id().value(),
                reservation.businessId().value(),
                reservation.resourceId().value(),
                reservation.customerAccountId().value(),
                reservation.startAt(),
                reservation.endAt(),
                reservation.stateAt(now),
                reservation.holdExpiresAt(),
                reservation.createdAt(),
                reservation.updatedAt());
    }
}
