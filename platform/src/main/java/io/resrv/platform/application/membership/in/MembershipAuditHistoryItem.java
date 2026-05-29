package io.resrv.platform.application.membership.in;

import io.resrv.platform.domain.membership.BusinessMembershipAuditEntry;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.platform.domain.membership.MembershipAuditAction;
import java.time.Instant;
import java.util.UUID;

public record MembershipAuditHistoryItem(
        UUID auditEntryId,
        UUID membershipId,
        MembershipAuditAction action,
        MembershipAccountSummary actor,
        MembershipAccountSummary target,
        MembershipState previousState,
        MembershipState newState,
        Instant occurredAt) {

    public static MembershipAuditHistoryItem from(
            final BusinessMembershipAuditEntry entry,
            final MembershipAccountSummary actor,
            final MembershipAccountSummary target) {
        return new MembershipAuditHistoryItem(
                entry.id(),
                entry.membershipId(),
                entry.action(),
                actor,
                target,
                state(entry.previousRole(), entry.previousActive()),
                state(entry.newRole(), entry.newActive()),
                entry.occurredAt());
    }

    private static MembershipState state(final BusinessRole role, final Boolean active) {
        if (role == null || active == null) {
            return null;
        }
        return new MembershipState(role, active);
    }
}
