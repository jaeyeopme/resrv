package io.resrv.adapter.in.web.tenant;

import io.resrv.domain.tenant.Tenant;
import java.time.Instant;
import java.util.UUID;

record TenantResponse(
        UUID id,
        String name,
        String slug,
        String timezone,
        int slotDuration,
        int holdTtl,
        int cancellationWindow,
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
