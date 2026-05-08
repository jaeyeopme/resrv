package io.resrv.domain.resource;

import io.resrv.domain.tenant.TenantId;

public final class ResourceSlugAlreadyExistsException extends RuntimeException {

    private final TenantId tenantId;
    private final ResourceSlug slug;

    public ResourceSlugAlreadyExistsException(final TenantId tenantId, final ResourceSlug slug) {
        super("Resource slug '%s' is already in use for this tenant".formatted(slug.value()));
        this.tenantId = tenantId;
        this.slug = slug;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public ResourceSlug slug() {
        return slug;
    }
}
