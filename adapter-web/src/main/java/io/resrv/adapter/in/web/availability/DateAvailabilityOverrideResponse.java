package io.resrv.adapter.in.web.availability;

import io.resrv.application.availability.in.DateAvailabilityOverrideResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Saved date-specific availability override.")
record DateAvailabilityOverrideResponse(
        @Schema(
                        description = "Date override identifier.",
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
        @Schema(description = "Tenant-local override date.", example = "2026-05-11") LocalDate date,
        @Schema(
                        description = "Whether the resource is closed for the whole date.",
                        example = "false")
                boolean closed,
        @Schema(
                        description = "Tenant-local override start time when not closed.",
                        example = "10:00")
                LocalTime startTime,
        @Schema(description = "Tenant-local override end time when not closed.", example = "16:00")
                LocalTime endTime,
        @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                Instant createdAt,
        @Schema(description = "Last update timestamp.", example = "2026-05-10T00:00:00Z")
                Instant updatedAt) {

    static DateAvailabilityOverrideResponse from(final DateAvailabilityOverrideResult result) {
        return new DateAvailabilityOverrideResponse(
                result.id(),
                result.tenantId(),
                result.resourceId(),
                result.date(),
                result.closed(),
                result.startTime(),
                result.endTime(),
                result.createdAt(),
                result.updatedAt());
    }
}
