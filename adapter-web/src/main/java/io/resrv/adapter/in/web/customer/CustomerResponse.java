package io.resrv.adapter.in.web.customer;

import io.resrv.application.customer.in.CustomerResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Registered customer.")
record CustomerResponse(
        @Schema(
                        description = "Customer identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                UUID id,
        @Schema(
                        description = "Tenant identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91d")
                UUID tenantId,
        @Schema(description = "Customer email.", example = "customer@example.com") String email,
        @Schema(description = "Customer display name.", example = "Jane Customer") String name,
        @Schema(
                        description = "Whether the customer can currently use the service.",
                        example = "true")
                boolean active,
        @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                Instant createdAt) {

    static CustomerResponse from(final CustomerResult result) {
        return new CustomerResponse(
                result.id(),
                result.tenantId(),
                result.email(),
                result.name(),
                result.active(),
                result.createdAt());
    }
}
