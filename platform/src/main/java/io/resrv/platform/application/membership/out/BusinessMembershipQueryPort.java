package io.resrv.platform.application.membership.out;

import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;

public interface BusinessMembershipQueryPort {

    Optional<BusinessMembership> findActiveByAccountIdAndBusinessId(
            AccountId accountId, BusinessId businessId);
}
