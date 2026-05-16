package io.resrv.adapter.in.web.availability;

import io.resrv.application.availability.in.WeeklyAvailabilityResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Saved recurring weekly availability window.")
record WeeklyAvailabilityResponse(
        @Schema(
                        description = "Weekly availability identifier.",
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
        @Schema(description = "Day of week for the recurring window.", example = "MONDAY")
                DayOfWeek dayOfWeek,
        @Schema(description = "Tenant-local opening time.", example = "09:00") LocalTime startTime,
        @Schema(description = "Tenant-local closing time.", example = "18:00") LocalTime endTime,
        @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                Instant createdAt,
        @Schema(description = "Last update timestamp.", example = "2026-05-10T00:00:00Z")
                Instant updatedAt) {

    static WeeklyAvailabilityResponse from(final WeeklyAvailabilityResult result) {
        return new WeeklyAvailabilityResponse(
                result.id(),
                result.tenantId(),
                result.resourceId(),
                result.dayOfWeek(),
                result.startTime(),
                result.endTime(),
                result.createdAt(),
                result.updatedAt());
    }
}
