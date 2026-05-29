package io.resrv.platform.domain.membership;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BusinessMembershipAuditEntry(
        UUID id,
        UUID membershipId,
        BusinessId businessId,
        AccountId actorAccountId,
        AccountId targetAccountId,
        MembershipAuditAction action,
        BusinessRole previousRole,
        Boolean previousActive,
        BusinessRole newRole,
        Boolean newActive,
        Instant occurredAt) {

    public BusinessMembershipAuditEntry {
        id = Objects.requireNonNull(id, "Membership audit entry id must not be null");
        membershipId =
                Objects.requireNonNull(
                        membershipId, "Membership audit membership id must not be null");
        businessId =
                Objects.requireNonNull(businessId, "Membership audit business id must not be null");
        actorAccountId =
                Objects.requireNonNull(
                        actorAccountId, "Membership audit actor account id must not be null");
        targetAccountId =
                Objects.requireNonNull(
                        targetAccountId, "Membership audit target account id must not be null");
        action = Objects.requireNonNull(action, "Membership audit action must not be null");
        occurredAt =
                Objects.requireNonNull(occurredAt, "Membership audit occurredAt must not be null");
    }

    public static BusinessMembershipAuditEntry create(
            final UUID membershipId,
            final BusinessId businessId,
            final AccountId actorAccountId,
            final AccountId targetAccountId,
            final MembershipAuditAction action,
            final BusinessRole previousRole,
            final Boolean previousActive,
            final BusinessRole newRole,
            final Boolean newActive,
            final Instant occurredAt) {
        return new BusinessMembershipAuditEntry(
                UUID.randomUUID(),
                membershipId,
                businessId,
                actorAccountId,
                targetAccountId,
                action,
                previousRole,
                previousActive,
                newRole,
                newActive,
                occurredAt);
    }
}
