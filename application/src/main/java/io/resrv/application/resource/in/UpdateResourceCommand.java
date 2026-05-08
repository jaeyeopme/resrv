package io.resrv.application.resource.in;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import org.jspecify.annotations.Nullable;

public record UpdateResourceCommand(
        TenantId tenantId,
        ResourceId resourceId,
        String name,
        String slug,
        @Nullable String description) {}
