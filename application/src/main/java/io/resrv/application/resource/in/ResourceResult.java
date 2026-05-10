package io.resrv.application.resource.in;

import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceStatus;
import java.time.Instant;
import java.util.UUID;

public record ResourceResult(
        UUID id,
        String name,
        String slug,
        String description,
        ResourceStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ResourceResult from(final Resource resource) {
        return new ResourceResult(
                resource.id().value(),
                resource.name().value(),
                resource.slug().value(),
                resource.description().value(),
                resource.status(),
                resource.createdAt(),
                resource.updatedAt());
    }
}
