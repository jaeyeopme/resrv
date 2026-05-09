package io.resrv.application.availability.in;

import io.resrv.domain.availability.DateAvailabilityOverride;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record DateAvailabilityOverrideResult(
        UUID id,
        UUID tenantId,
        UUID resourceId,
        LocalDate date,
        boolean closed,
        @Nullable LocalTime startTime,
        @Nullable LocalTime endTime,
        Instant createdAt,
        Instant updatedAt) {

    public static DateAvailabilityOverrideResult from(final DateAvailabilityOverride override) {
        return new DateAvailabilityOverrideResult(
                override.id().value(),
                override.tenantId().value(),
                override.resourceId().value(),
                override.date(),
                override.closed(),
                override.startTime(),
                override.endTime(),
                override.createdAt(),
                override.updatedAt());
    }
}
