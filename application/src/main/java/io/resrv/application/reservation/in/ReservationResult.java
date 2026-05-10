package io.resrv.application.reservation.in;

import io.resrv.domain.reservation.Reservation;
import io.resrv.domain.reservation.ReservationStatus;
import java.time.Instant;
import java.util.UUID;

public record ReservationResult(
        UUID id,
        UUID tenantId,
        UUID resourceId,
        UUID customerId,
        Instant startAt,
        Instant endAt,
        ReservationStatus status,
        Instant holdExpiresAt,
        Instant createdAt,
        Instant updatedAt,
        Instant confirmedAt,
        Instant cancelledAt) {

    public static ReservationResult from(final Reservation reservation) {
        return new ReservationResult(
                reservation.id().value(),
                reservation.tenantId().value(),
                reservation.resourceId().value(),
                reservation.customerId().value(),
                reservation.startAt(),
                reservation.endAt(),
                reservation.status(),
                reservation.holdExpiresAt(),
                reservation.createdAt(),
                reservation.updatedAt(),
                reservation.confirmedAt(),
                reservation.cancelledAt());
    }
}
