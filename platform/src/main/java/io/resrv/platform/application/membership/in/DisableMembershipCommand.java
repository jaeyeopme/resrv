package io.resrv.platform.application.membership.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.util.UUID;

public record DisableMembershipCommand(
        AccountId actorAccountId, BusinessId businessId, UUID membershipId) {}
