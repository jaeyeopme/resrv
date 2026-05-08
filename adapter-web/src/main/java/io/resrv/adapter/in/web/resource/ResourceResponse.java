package io.resrv.adapter.in.web.resource;

import io.resrv.application.resource.in.ResourceResult;
import io.resrv.domain.resource.ResourceStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

record ResourceResponse(
        UUID id,
        String name,
        String slug,
        @Nullable String description,
        ResourceStatus status,
        Instant createdAt,
        Instant updatedAt) {

    static ResourceResponse from(final ResourceResult result) {
        return new ResourceResponse(
                result.id(),
                result.name(),
                result.slug(),
                result.description(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }
}
