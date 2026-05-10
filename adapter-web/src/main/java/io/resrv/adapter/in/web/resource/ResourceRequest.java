package io.resrv.adapter.in.web.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        description =
                "Resource create/update payload. Tenant id is taken from the JWT, not the body.")
record ResourceRequest(
        @Schema(description = "Human-readable resource name.", example = "Room A")
                @NotBlank(message = "Resource name is required")
                @Size(max = 100, message = "Resource name must be at most 100 characters")
                String name,
        @Schema(description = "Tenant-scoped resource slug.", example = "room-a")
                @NotBlank(message = "Resource slug is required")
                @Size(min = 3, max = 63, message = "Resource slug must be 3-63 characters")
                String slug,
        @Schema(description = "Optional resource description.", example = "Consulting room")
                @Size(max = 500, message = "Resource description must be at most 500 characters")
                String description) {}
