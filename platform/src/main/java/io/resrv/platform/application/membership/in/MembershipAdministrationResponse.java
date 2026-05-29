package io.resrv.platform.application.membership.in;

import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessRole;
import java.time.Instant;
import java.util.UUID;

public record MembershipAdministrationResponse(
        UUID membershipId,
        UUID businessId,
        MembershipAccountSummary account,
        BusinessRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant disabledAt) {

    public static MembershipAdministrationResponse from(
            final BusinessMembership membership, final MembershipAccountSummary account) {
        return new MembershipAdministrationResponse(
                membership.id(),
                membership.businessId().value(),
                account,
                membership.role(),
                membership.active(),
                membership.createdAt(),
                membership.updatedAt(),
                membership.disabledAt());
    }
}
