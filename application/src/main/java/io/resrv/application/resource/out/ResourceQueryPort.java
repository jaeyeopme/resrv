package io.resrv.application.resource.out;

import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceSlug;
import io.resrv.domain.resource.ResourceStatus;
import io.resrv.domain.tenant.TenantId;
import java.util.List;
import java.util.Optional;

public interface ResourceQueryPort {

    Optional<Resource> findByTenantIdAndId(final TenantId tenantId, final ResourceId resourceId);

    Optional<Resource> findByTenantIdAndSlug(final TenantId tenantId, final ResourceSlug slug);

    boolean existsByTenantIdAndSlug(final TenantId tenantId, final ResourceSlug slug);

    List<Resource> findByTenantIdAndStatus(final TenantId tenantId, final ResourceStatus status);
}
