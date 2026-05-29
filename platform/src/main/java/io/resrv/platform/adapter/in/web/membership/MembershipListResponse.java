package io.resrv.platform.adapter.in.web.membership;

import io.resrv.platform.application.membership.in.BusinessMembershipListItem;
import io.resrv.platform.domain.membership.BusinessRole;
import java.time.Instant;
import java.util.UUID;

record MembershipListResponse(
        UUID membershipId,
        UUID businessId,
        MembershipResponse.AccountResponse account,
        BusinessRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant disabledAt) {

    static MembershipListResponse from(final BusinessMembershipListItem item) {
        return new MembershipListResponse(
                item.membershipId(),
                item.businessId(),
                MembershipResponse.AccountResponse.from(item.account()),
                item.role(),
                item.active(),
                item.createdAt(),
                item.updatedAt(),
                item.disabledAt());
    }
}
