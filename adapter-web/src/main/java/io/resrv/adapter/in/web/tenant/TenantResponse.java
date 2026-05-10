package io.resrv.adapter.in.web.tenant;

import io.resrv.domain.tenant.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Registered tenant and its scheduling defaults.")
record TenantResponse(
        @Schema(
                        description = "Tenant identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                UUID id,
        @Schema(description = "Tenant display name.", example = "Demo Studio") String name,
        @Schema(description = "Public tenant slug.", example = "demo-studio") String slug,
        @Schema(
                        description = "IANA timezone used for availability calculation.",
                        example = "Asia/Seoul")
                String timezone,
        @Schema(description = "Default slot duration in minutes.", example = "60") int slotDuration,
        @Schema(description = "Reservation hold TTL in minutes.", example = "15") int holdTtl,
        @Schema(description = "Cancellation window in minutes.", example = "60")
                int cancellationWindow,
        @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                Instant createdAt) {

    static TenantResponse from(final Tenant tenant) {
        return new TenantResponse(
                tenant.id().value(),
                tenant.name().value(),
                tenant.slug().value(),
                tenant.timezone().value().getId(),
                tenant.slotDuration().minutes(),
                tenant.holdTtl().minutes(),
                tenant.cancellationWindow().minutes(),
                tenant.createdAt());
    }
}
