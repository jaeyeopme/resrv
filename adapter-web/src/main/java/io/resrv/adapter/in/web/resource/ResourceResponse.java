package io.resrv.adapter.in.web.resource;

import io.resrv.application.resource.in.ResourceResult;
import io.resrv.domain.resource.ResourceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Schema(description = "Tenant-scoped reservable resource.")
record ResourceResponse(
        @Schema(
                        description = "Resource identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                UUID id,
        @Schema(description = "Human-readable resource name.", example = "Room A") String name,
        @Schema(description = "Tenant-scoped resource slug.", example = "room-a") String slug,
        @Schema(description = "Optional resource description.", example = "Consulting room")
                @Nullable String description,
        @Schema(description = "Resource lifecycle status.", example = "ACTIVE")
                ResourceStatus status,
        @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                Instant createdAt,
        @Schema(description = "Last update timestamp.", example = "2026-05-10T00:00:00Z")
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
