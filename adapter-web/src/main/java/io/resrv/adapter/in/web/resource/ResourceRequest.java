package io.resrv.adapter.in.web.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

record ResourceRequest(
        @NotBlank(message = "Resource name is required")
                @Size(max = 100, message = "Resource name must be at most 100 characters")
                String name,
        @NotBlank(message = "Resource slug is required")
                @Size(min = 3, max = 63, message = "Resource slug must be 3-63 characters")
                String slug,
        @Nullable @Size(max = 500, message = "Resource description must be at most 500 characters")
                String description) {}
