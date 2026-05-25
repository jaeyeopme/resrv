package io.resrv.platform.application.membership;

import io.resrv.platform.application.membership.in.CheckBusinessAccessUseCase;
import io.resrv.platform.application.membership.out.BusinessMembershipQueryPort;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CheckBusinessAccessService implements CheckBusinessAccessUseCase {

    private final BusinessMembershipQueryPort membershipQueryPort;

    public CheckBusinessAccessService(final BusinessMembershipQueryPort membershipQueryPort) {
        this.membershipQueryPort = membershipQueryPort;
    }

    @Override
    public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
        return membershipQueryPort
                .findActiveByAccountIdAndBusinessId(accountId, businessId)
                .map(
                        membership ->
                                membership.role() == BusinessRole.OWNER
                                        || membership.role() == BusinessRole.STAFF)
                .orElse(false);
    }
}
