package io.resrv.platform.application.membership.out;

import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessMembershipAuditEntry;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessMembershipQueryPort {

    Optional<BusinessMembership> findActiveByAccountIdAndBusinessId(
            AccountId accountId, BusinessId businessId);

    Optional<BusinessMembership> findByAccountIdAndBusinessId(
            AccountId accountId, BusinessId businessId);

    Optional<BusinessMembership> findById(UUID membershipId);

    List<BusinessMembership> findByBusinessId(BusinessId businessId);

    long countActiveByBusinessIdAndRole(BusinessId businessId, BusinessRole role);

    List<BusinessMembershipAuditEntry> findAuditEntriesByBusinessId(BusinessId businessId);
}
