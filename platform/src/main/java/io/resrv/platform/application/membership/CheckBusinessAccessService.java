package io.resrv.platform.application.membership;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.application.membership.out.BusinessMembershipQueryPort;
import io.resrv.platform.contract.membership.BusinessAccessCheck;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CheckBusinessAccessService implements BusinessAccessCheck {

    private static final Logger log = LoggerFactory.getLogger(CheckBusinessAccessService.class);
    private static final String INACTIVE_OR_INSUFFICIENT_MEMBERSHIP_DENIAL =
            "Denied business access for inactive or insufficient membership: accountId={}, businessId={}";

    private final AccountQueryPort accountQueryPort;
    private final BusinessQueryPort businessQueryPort;
    private final BusinessMembershipQueryPort membershipQueryPort;

    public CheckBusinessAccessService(
            final AccountQueryPort accountQueryPort,
            final BusinessQueryPort businessQueryPort,
            final BusinessMembershipQueryPort membershipQueryPort) {
        this.accountQueryPort = accountQueryPort;
        this.businessQueryPort = businessQueryPort;
        this.membershipQueryPort = membershipQueryPort;
    }

    @Override
    public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
        if (accountQueryPort.findById(accountId).filter(account -> account.active()).isEmpty()) {
            log.info(
                    "Denied business access for inactive account: accountId={}, businessId={}",
                    accountId.value(),
                    businessId.value());
            return false;
        }
        if (businessQueryPort.findById(businessId).filter(Business::active).isEmpty()) {
            log.info(
                    "Denied business access for inactive business: accountId={}, businessId={}",
                    accountId.value(),
                    businessId.value());
            return false;
        }
        return membershipQueryPort
                .findActiveByAccountIdAndBusinessId(accountId, businessId)
                .map(
                        membership ->
                                membership.role() == BusinessRole.OWNER
                                        || membership.role() == BusinessRole.STAFF)
                .orElseGet(
                        () -> {
                            log.info(
                                    INACTIVE_OR_INSUFFICIENT_MEMBERSHIP_DENIAL,
                                    accountId.value(),
                                    businessId.value());
                            return false;
                        });
    }
}
