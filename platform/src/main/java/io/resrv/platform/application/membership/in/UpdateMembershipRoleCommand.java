package io.resrv.platform.application.membership.in;

import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.util.UUID;

public record UpdateMembershipRoleCommand(
        AccountId actorAccountId, BusinessId businessId, UUID membershipId, BusinessRole role) {}
