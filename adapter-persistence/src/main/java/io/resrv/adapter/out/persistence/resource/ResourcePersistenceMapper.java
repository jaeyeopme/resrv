package io.resrv.adapter.out.persistence.resource;

import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceDescription;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceName;
import io.resrv.domain.resource.ResourceSlug;
import io.resrv.domain.resource.ResourceStatus;
import io.resrv.domain.tenant.TenantId;

final class ResourcePersistenceMapper {

    private ResourcePersistenceMapper() {}

    static ResourceJpaEntity toJpaEntity(final Resource resource) {
        return new ResourceJpaEntity(
                resource.id().value(),
                resource.tenantId().value(),
                resource.slug().value(),
                resource.name().value(),
                resource.description().value(),
                resource.status().name(),
                resource.createdAt(),
                resource.updatedAt());
    }

    static Resource toDomain(final ResourceJpaEntity entity) {
        return Resource.reconstitute(
                ResourceId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                new ResourceName(entity.getName()),
                new ResourceSlug(entity.getSlug()),
                new ResourceDescription(entity.getDescription()),
                ResourceStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
