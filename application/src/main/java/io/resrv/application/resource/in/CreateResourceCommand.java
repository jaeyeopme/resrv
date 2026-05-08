package io.resrv.application.resource.in;

import io.resrv.domain.tenant.TenantId;
import org.jspecify.annotations.Nullable;

public record CreateResourceCommand(
        TenantId tenantId, String name, String slug, @Nullable String description) {}
