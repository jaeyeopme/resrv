package io.resrv.adapter.out.persistence.resource;

import io.resrv.application.resource.out.ResourceCommandPort;
import io.resrv.application.resource.out.ResourceQueryPort;
import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceSlug;
import io.resrv.domain.resource.ResourceSlugAlreadyExistsException;
import io.resrv.domain.resource.ResourceStatus;
import io.resrv.domain.tenant.TenantId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ResourcePersistenceAdapter implements ResourceCommandPort, ResourceQueryPort {

    private final ResourceJpaRepository resourceJpaRepository;
    private final EntityManager entityManager;

    ResourcePersistenceAdapter(
            final ResourceJpaRepository resourceJpaRepository, final EntityManager entityManager) {
        this.resourceJpaRepository = resourceJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(final Resource resource) {
        try {
            resourceJpaRepository.save(ResourcePersistenceMapper.toJpaEntity(resource));
            entityManager.flush();
        } catch (final PersistenceException _) {
            // The only unique resource constraint is tenant-scoped slug.
            throw new ResourceSlugAlreadyExistsException(resource.tenantId(), resource.slug());
        }
    }

    @Override
    public Optional<Resource> findByTenantIdAndId(
            final TenantId tenantId, final ResourceId resourceId) {
        return resourceJpaRepository
                .findByTenantIdAndId(tenantId.value(), resourceId.value())
                .map(ResourcePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Resource> findByTenantIdAndSlug(
            final TenantId tenantId, final ResourceSlug slug) {
        return resourceJpaRepository
                .findByTenantIdAndSlug(tenantId.value(), slug.value())
                .map(ResourcePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByTenantIdAndSlug(final TenantId tenantId, final ResourceSlug slug) {
        return resourceJpaRepository.existsByTenantIdAndSlug(tenantId.value(), slug.value());
    }

    @Override
    public List<Resource> findByTenantIdAndStatus(
            final TenantId tenantId, final ResourceStatus status) {
        return resourceJpaRepository
                .findByTenantIdAndStatus(tenantId.value(), status.name())
                .stream()
                .map(ResourcePersistenceMapper::toDomain)
                .toList();
    }
}
