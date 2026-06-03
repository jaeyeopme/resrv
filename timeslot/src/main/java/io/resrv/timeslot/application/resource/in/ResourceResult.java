package io.resrv.timeslot.application.resource.in;

import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import java.time.Instant;
import java.util.UUID;

public record ResourceResult(
        UUID id,
        UUID businessId,
        String name,
        String description,
        ResourceStatus status,
        Integer slotDurationMinutes,
        Integer holdTtlMinutes,
        Integer cancellationWindowMinutes,
        Instant createdAt,
        Instant updatedAt) {

    public static ResourceResult from(final Resource resource) {
        final var overrides = resource.bookingOverrides();
        return new ResourceResult(
                resource.id().value(),
                resource.businessId().value(),
                resource.name().value(),
                resource.description(),
                resource.status(),
                overrides.slotDuration() == null ? null : overrides.slotDuration().minutes(),
                overrides.holdTtl() == null ? null : overrides.holdTtl().minutes(),
                overrides.cancellationWindow() == null
                        ? null
                        : overrides.cancellationWindow().minutes(),
                resource.createdAt(),
                resource.updatedAt());
    }
}
