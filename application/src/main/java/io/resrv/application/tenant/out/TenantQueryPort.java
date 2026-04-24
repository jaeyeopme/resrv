package io.resrv.application.tenant.out;

import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantId;
import java.util.Optional;

public interface TenantQueryPort {

    boolean existsBySlug(final String slug);

    Optional<TenantId> findIdBySlug(final String slug);

    Optional<Tenant> findById(final TenantId tenantId);

    Optional<Tenant> findBySlug(final String slug);
}
