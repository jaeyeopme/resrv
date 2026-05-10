package io.resrv.application.resource.in;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;

public record UpdateResourceCommand(
        TenantId tenantId, ResourceId resourceId, String name, String slug, String description) {}
