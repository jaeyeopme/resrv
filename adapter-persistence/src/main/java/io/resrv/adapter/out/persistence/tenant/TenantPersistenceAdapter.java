package io.resrv.adapter.out.persistence.tenant;

import io.resrv.application.tenant.out.TenantCommandPort;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.tenant.SlugAlreadyExistsException;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class TenantPersistenceAdapter implements TenantCommandPort, TenantQueryPort {

    private final TenantJpaRepository tenantJpaRepository;
    private final EntityManager entityManager;

    TenantPersistenceAdapter(
            final TenantJpaRepository tenantJpaRepository, final EntityManager entityManager) {
        this.tenantJpaRepository = tenantJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(final Tenant tenant) {
        try {
            tenantJpaRepository.save(TenantPersistenceMapper.toJpaEntity(tenant));
            entityManager.flush();
        } catch (final PersistenceException _) {
            // Only slug has a unique constraint; re-throw if another constraint is added later
            throw new SlugAlreadyExistsException(tenant.slug());
        }
    }

    @Override
    public boolean existsBySlug(final String slug) {
        return tenantJpaRepository.existsBySlug(slug);
    }

    @Override
    public Optional<TenantId> findIdBySlug(final String slug) {
        return tenantJpaRepository.findIdBySlug(slug).map(TenantId::of);
    }

    @Override
    public Optional<Tenant> findById(final TenantId tenantId) {
        return tenantJpaRepository
                .findById(tenantId.value())
                .map(TenantPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Tenant> findBySlug(final String slug) {
        return tenantJpaRepository.findBySlug(slug).map(TenantPersistenceMapper::toDomain);
    }
}
