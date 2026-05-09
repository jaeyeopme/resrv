package io.resrv.application.availability.in;

import io.resrv.domain.availability.WeeklyAvailability;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record WeeklyAvailabilityResult(
        UUID id,
        UUID tenantId,
        UUID resourceId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Instant createdAt,
        Instant updatedAt) {

    public static WeeklyAvailabilityResult from(final WeeklyAvailability availability) {
        return new WeeklyAvailabilityResult(
                availability.id().value(),
                availability.tenantId().value(),
                availability.resourceId().value(),
                availability.dayOfWeek(),
                availability.startTime(),
                availability.endTime(),
                availability.createdAt(),
                availability.updatedAt());
    }
}
