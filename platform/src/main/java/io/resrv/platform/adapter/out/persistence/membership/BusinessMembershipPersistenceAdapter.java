package io.resrv.platform.adapter.out.persistence.membership;

import io.resrv.platform.application.membership.out.BusinessMembershipCommandPort;
import io.resrv.platform.application.membership.out.BusinessMembershipQueryPort;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessMembershipAuditEntry;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class BusinessMembershipPersistenceAdapter
        implements BusinessMembershipCommandPort, BusinessMembershipQueryPort {

    private final BusinessMembershipJpaRepository businessMembershipJpaRepository;
    private final BusinessMembershipAuditEntryJpaRepository auditEntryJpaRepository;
    private final EntityManager entityManager;

    BusinessMembershipPersistenceAdapter(
            final BusinessMembershipJpaRepository businessMembershipJpaRepository,
            final BusinessMembershipAuditEntryJpaRepository auditEntryJpaRepository,
            final EntityManager entityManager) {
        this.businessMembershipJpaRepository = businessMembershipJpaRepository;
        this.auditEntryJpaRepository = auditEntryJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(final BusinessMembership membership) {
        businessMembershipJpaRepository.save(BusinessMembershipJpaEntity.fromDomain(membership));
        entityManager.flush();
    }

    @Override
    public void saveAuditEntry(final BusinessMembershipAuditEntry auditEntry) {
        auditEntryJpaRepository.save(BusinessMembershipAuditEntryJpaEntity.fromDomain(auditEntry));
        entityManager.flush();
    }

    @Override
    public Optional<BusinessMembership> findActiveByAccountIdAndBusinessId(
            final AccountId accountId, final BusinessId businessId) {
        return businessMembershipJpaRepository
                .findByAccountIdAndBusinessIdAndActiveTrue(accountId.value(), businessId.value())
                .map(BusinessMembershipJpaEntity::toDomain);
    }

    @Override
    public Optional<BusinessMembership> findByAccountIdAndBusinessId(
            final AccountId accountId, final BusinessId businessId) {
        return businessMembershipJpaRepository
                .findByAccountIdAndBusinessId(accountId.value(), businessId.value())
                .map(BusinessMembershipJpaEntity::toDomain);
    }

    @Override
    public Optional<BusinessMembership> findById(final UUID membershipId) {
        return businessMembershipJpaRepository
                .findById(membershipId)
                .map(BusinessMembershipJpaEntity::toDomain);
    }

    @Override
    public List<BusinessMembership> findByBusinessId(final BusinessId businessId) {
        return businessMembershipJpaRepository
                .findByBusinessIdOrderByCreatedAtAsc(businessId.value())
                .stream()
                .map(BusinessMembershipJpaEntity::toDomain)
                .toList();
    }

    @Override
    public long countActiveByBusinessIdAndRole(
            final BusinessId businessId, final BusinessRole role) {
        return businessMembershipJpaRepository.countByBusinessIdAndRoleAndActiveTrue(
                businessId.value(), role);
    }

    @Override
    public List<BusinessMembershipAuditEntry> findAuditEntriesByBusinessId(
            final BusinessId businessId) {
        return auditEntryJpaRepository
                .findByBusinessIdOrderByOccurredAtDesc(businessId.value())
                .stream()
                .map(BusinessMembershipAuditEntryJpaEntity::toDomain)
                .toList();
    }
}
