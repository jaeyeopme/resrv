package io.resrv.platform.adapter.out.persistence.membership;

import io.resrv.platform.application.membership.out.BusinessMembershipCommandPort;
import io.resrv.platform.application.membership.out.BusinessMembershipQueryPort;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class BusinessMembershipPersistenceAdapter
        implements BusinessMembershipCommandPort, BusinessMembershipQueryPort {

    private final BusinessMembershipJpaRepository businessMembershipJpaRepository;
    private final EntityManager entityManager;

    BusinessMembershipPersistenceAdapter(
            final BusinessMembershipJpaRepository businessMembershipJpaRepository,
            final EntityManager entityManager) {
        this.businessMembershipJpaRepository = businessMembershipJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(final BusinessMembership membership) {
        businessMembershipJpaRepository.save(BusinessMembershipJpaEntity.fromDomain(membership));
        entityManager.flush();
    }

    @Override
    public Optional<BusinessMembership> findActiveByAccountIdAndBusinessId(
            final AccountId accountId, final BusinessId businessId) {
        return businessMembershipJpaRepository
                .findByAccountIdAndBusinessIdAndActiveTrue(accountId.value(), businessId.value())
                .map(BusinessMembershipJpaEntity::toDomain);
    }
}
