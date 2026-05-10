package io.resrv.application.resource.in;

import io.resrv.domain.tenant.TenantId;

public record CreateResourceCommand(
        TenantId tenantId, String name, String slug, String description) {}
