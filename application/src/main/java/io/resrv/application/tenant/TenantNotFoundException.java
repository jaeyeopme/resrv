package io.resrv.application.tenant;

import io.resrv.domain.tenant.TenantId;

public final class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(final TenantId tenantId) {
        super("Tenant '%s' was not found".formatted(tenantId.value()));
    }

    public TenantNotFoundException(final String tenantSlug) {
        super("Tenant '%s' was not found".formatted(tenantSlug));
    }
}
