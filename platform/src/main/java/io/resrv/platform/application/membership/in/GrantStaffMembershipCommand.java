package io.resrv.platform.application.membership.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;

public record GrantStaffMembershipCommand(
        AccountId actorAccountId, BusinessId businessId, String targetAccountEmail) {}
