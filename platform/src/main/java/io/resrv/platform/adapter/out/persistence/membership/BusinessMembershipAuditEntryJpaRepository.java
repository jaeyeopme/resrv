package io.resrv.platform.adapter.out.persistence.membership;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface BusinessMembershipAuditEntryJpaRepository
        extends CrudRepository<BusinessMembershipAuditEntryJpaEntity, UUID> {

    List<BusinessMembershipAuditEntryJpaEntity> findByBusinessIdOrderByOccurredAtDesc(
            UUID businessId);
}
