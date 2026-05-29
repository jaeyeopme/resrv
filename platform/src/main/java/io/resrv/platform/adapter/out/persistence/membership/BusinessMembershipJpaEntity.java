package io.resrv.platform.adapter.out.persistence.membership;

import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessRole;
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
@Table(schema = "platform", name = "business_membership")
class BusinessMembershipJpaEntity {

    @Id private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BusinessRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    protected BusinessMembershipJpaEntity() {}

    BusinessMembershipJpaEntity(
            final UUID id,
            final UUID accountId,
            final UUID businessId,
            final BusinessRole role,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant disabledAt) {
        this.id = id;
        this.accountId = accountId;
        this.businessId = businessId;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.disabledAt = disabledAt;
    }

    static BusinessMembershipJpaEntity fromDomain(final BusinessMembership membership) {
        return new BusinessMembershipJpaEntity(
                membership.id(),
                membership.accountId().value(),
                membership.businessId().value(),
                membership.role(),
                membership.active(),
                membership.createdAt(),
                membership.updatedAt(),
                membership.disabledAt());
    }

    BusinessMembership toDomain() {
        return BusinessMembership.reconstitute(
                id,
                AccountId.of(accountId),
                BusinessId.of(businessId),
                role,
                active,
                createdAt,
                updatedAt,
                disabledAt);
    }
}
