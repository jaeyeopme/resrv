package io.resrv.domain.resource;

import io.resrv.domain.tenant.TenantId;

public final class ResourceNotFoundException extends RuntimeException {

    private final TenantId tenantId;
    private final ResourceId resourceId;

    public ResourceNotFoundException(final TenantId tenantId, final ResourceId resourceId) {
        super("Resource '%s' was not found".formatted(resourceId.value()));
        this.tenantId = tenantId;
        this.resourceId = resourceId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public ResourceId resourceId() {
        return resourceId;
    }
}
