package io.resrv.platform.adapter.out.persistence.membership;

import io.resrv.platform.domain.membership.BusinessMembershipAuditEntry;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.platform.domain.membership.MembershipAuditAction;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "platform", name = "business_membership_audit_entry")
class BusinessMembershipAuditEntryJpaEntity {

    @Id private UUID id;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "actor_account_id", nullable = false)
    private UUID actorAccountId;

    @Column(name = "target_account_id", nullable = false)
    private UUID targetAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private MembershipAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_role", length = 32)
    private BusinessRole previousRole;

    @Column(name = "previous_active")
    private Boolean previousActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_role", length = 32)
    private BusinessRole newRole;

    @Column(name = "new_active")
    private Boolean newActive;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected BusinessMembershipAuditEntryJpaEntity() {}

    BusinessMembershipAuditEntryJpaEntity(
            final UUID id,
            final UUID membershipId,
            final UUID businessId,
            final UUID actorAccountId,
            final UUID targetAccountId,
            final MembershipAuditAction action,
            final BusinessRole previousRole,
            final Boolean previousActive,
            final BusinessRole newRole,
            final Boolean newActive,
            final Instant occurredAt) {
        this.id = id;
        this.membershipId = membershipId;
        this.businessId = businessId;
        this.actorAccountId = actorAccountId;
        this.targetAccountId = targetAccountId;
        this.action = action;
        this.previousRole = previousRole;
        this.previousActive = previousActive;
        this.newRole = newRole;
        this.newActive = newActive;
        this.occurredAt = occurredAt;
    }

    static BusinessMembershipAuditEntryJpaEntity fromDomain(
            final BusinessMembershipAuditEntry entry) {
        return new BusinessMembershipAuditEntryJpaEntity(
                entry.id(),
                entry.membershipId(),
                entry.businessId().value(),
                entry.actorAccountId().value(),
                entry.targetAccountId().value(),
                entry.action(),
                entry.previousRole(),
                entry.previousActive(),
                entry.newRole(),
                entry.newActive(),
                entry.occurredAt());
    }

    BusinessMembershipAuditEntry toDomain() {
        return new BusinessMembershipAuditEntry(
                id,
                membershipId,
                BusinessId.of(businessId),
                AccountId.of(actorAccountId),
                AccountId.of(targetAccountId),
                action,
                previousRole,
                previousActive,
                newRole,
                newActive,
                occurredAt);
    }
}
