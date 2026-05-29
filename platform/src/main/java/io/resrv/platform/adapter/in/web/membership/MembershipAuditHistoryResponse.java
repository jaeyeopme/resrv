package io.resrv.platform.adapter.in.web.membership;

import io.resrv.platform.application.membership.in.MembershipAuditHistoryItem;
import io.resrv.platform.application.membership.in.MembershipState;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.platform.domain.membership.MembershipAuditAction;
import java.time.Instant;
import java.util.UUID;

record MembershipAuditHistoryResponse(
        UUID auditEntryId,
        UUID membershipId,
        MembershipAuditAction action,
        MembershipResponse.AccountResponse actor,
        MembershipResponse.AccountResponse target,
        StateResponse previousState,
        StateResponse newState,
        Instant occurredAt) {

    static MembershipAuditHistoryResponse from(final MembershipAuditHistoryItem item) {
        return new MembershipAuditHistoryResponse(
                item.auditEntryId(),
                item.membershipId(),
                item.action(),
                MembershipResponse.AccountResponse.from(item.actor()),
                MembershipResponse.AccountResponse.from(item.target()),
                StateResponse.from(item.previousState()),
                StateResponse.from(item.newState()),
                item.occurredAt());
    }

    record StateResponse(BusinessRole role, boolean active) {

        static StateResponse from(final MembershipState state) {
            if (state == null) {
                return null;
            }
            return new StateResponse(state.role(), state.active());
        }
    }
}
