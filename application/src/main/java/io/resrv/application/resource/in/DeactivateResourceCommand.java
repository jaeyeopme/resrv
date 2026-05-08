package io.resrv.application.resource.in;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;

public record DeactivateResourceCommand(TenantId tenantId, ResourceId resourceId) {}
