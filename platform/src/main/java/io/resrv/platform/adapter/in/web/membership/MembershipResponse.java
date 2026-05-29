package io.resrv.platform.adapter.in.web.membership;

import io.resrv.platform.application.membership.in.MembershipAccountSummary;
import io.resrv.platform.application.membership.in.MembershipAdministrationResponse;
import io.resrv.platform.domain.membership.BusinessRole;
import java.time.Instant;
import java.util.UUID;

record MembershipResponse(
        UUID membershipId,
        UUID businessId,
        AccountResponse account,
        BusinessRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant disabledAt) {

    static MembershipResponse from(final MembershipAdministrationResponse response) {
        return new MembershipResponse(
                response.membershipId(),
                response.businessId(),
                AccountResponse.from(response.account()),
                response.role(),
                response.active(),
                response.createdAt(),
                response.updatedAt(),
                response.disabledAt());
    }

    record AccountResponse(UUID accountId, String email, String name) {

        static AccountResponse from(final MembershipAccountSummary summary) {
            return new AccountResponse(summary.accountId(), summary.email(), summary.name());
        }
    }
}
