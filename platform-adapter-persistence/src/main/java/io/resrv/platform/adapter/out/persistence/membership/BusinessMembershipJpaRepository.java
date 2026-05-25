package io.resrv.platform.adapter.out.persistence.membership;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface BusinessMembershipJpaRepository
        extends CrudRepository<BusinessMembershipJpaEntity, UUID> {

    Optional<BusinessMembershipJpaEntity> findByAccountIdAndBusinessIdAndActiveTrue(
            UUID accountId, UUID businessId);
}
