package io.resrv.adapter.in.web.reservation;

import io.resrv.application.reservation.in.ReservationResult;
import io.resrv.domain.reservation.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Schema(description = "Reservation response.")
public record ReservationResponse(
        @Schema(
                        description = "Reservation identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                UUID id,
        @Schema(
                        description = "Tenant identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91d")
                UUID tenantId,
        @Schema(
                        description = "Resource identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91e")
                UUID resourceId,
        @Schema(
                        description = "Customer identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91f")
                UUID customerId,
        @Schema(description = "Reservation start instant.", example = "2026-05-11T01:00:00Z")
                Instant startAt,
        @Schema(description = "Reservation end instant.", example = "2026-05-11T02:00:00Z")
                Instant endAt,
        @Schema(description = "Reservation lifecycle state.", example = "HELD")
                ReservationStatus status,
        @Schema(
                        description = "Hold expiration timestamp for unconfirmed holds.",
                        example = "2026-05-11T00:10:00Z")
                @Nullable Instant holdExpiresAt,
        @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                Instant createdAt,
        @Schema(description = "Last update timestamp.", example = "2026-05-10T00:00:00Z")
                Instant updatedAt,
        @Schema(
                        description = "Confirmation timestamp, if confirmed.",
                        example = "2026-05-10T00:01:00Z")
                @Nullable Instant confirmedAt,
        @Schema(
                        description = "Cancellation timestamp, if cancelled.",
                        example = "2026-05-10T00:02:00Z")
                @Nullable Instant cancelledAt) {

    public static ReservationResponse from(final ReservationResult result) {
        return new ReservationResponse(
                result.id(),
                result.tenantId(),
                result.resourceId(),
                result.customerId(),
                result.startAt(),
                result.endAt(),
                result.status(),
                result.holdExpiresAt(),
                result.createdAt(),
                result.updatedAt(),
                result.confirmedAt(),
                result.cancelledAt());
    }
}
