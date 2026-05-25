package io.resrv.platform.adapter.out.persistence.business;

import io.resrv.platform.application.business.out.BusinessCommandPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.business.BusinessSlugAlreadyExistsException;
import io.resrv.shared.kernel.BusinessId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class BusinessPersistenceAdapter implements BusinessCommandPort, BusinessQueryPort {

    private final BusinessJpaRepository businessJpaRepository;
    private final EntityManager entityManager;

    BusinessPersistenceAdapter(
            final BusinessJpaRepository businessJpaRepository, final EntityManager entityManager) {
        this.businessJpaRepository = businessJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(final Business business) {
        try {
            businessJpaRepository.save(BusinessJpaEntity.fromDomain(business));
            entityManager.flush();
        } catch (final DataIntegrityViolationException | PersistenceException _) {
            throw new BusinessSlugAlreadyExistsException(business.slug());
        }
    }

    @Override
    public Optional<Business> findById(final BusinessId businessId) {
        return businessJpaRepository.findById(businessId.value()).map(BusinessJpaEntity::toDomain);
    }

    @Override
    public Optional<Business> findBySlug(final BusinessSlug slug) {
        return businessJpaRepository.findBySlug(slug.value()).map(BusinessJpaEntity::toDomain);
    }
}
