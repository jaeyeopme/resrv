package io.resrv.platform.adapter.out.persistence.membership;

import io.resrv.platform.domain.membership.BusinessRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface BusinessMembershipJpaRepository
        extends CrudRepository<BusinessMembershipJpaEntity, UUID> {

    Optional<BusinessMembershipJpaEntity> findByAccountIdAndBusinessIdAndActiveTrue(
            UUID accountId, UUID businessId);

    Optional<BusinessMembershipJpaEntity> findByAccountIdAndBusinessId(
            UUID accountId, UUID businessId);

    List<BusinessMembershipJpaEntity> findByBusinessIdOrderByCreatedAtAsc(UUID businessId);

    long countByBusinessIdAndRoleAndActiveTrue(UUID businessId, BusinessRole role);
}
