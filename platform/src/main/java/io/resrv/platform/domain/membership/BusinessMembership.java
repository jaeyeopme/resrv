package io.resrv.platform.domain.membership;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class BusinessMembership {

    private final UUID id;
    private final AccountId accountId;
    private final BusinessId businessId;
    private final BusinessRole role;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant disabledAt;

    private BusinessMembership(
            final UUID id,
            final AccountId accountId,
            final BusinessId businessId,
            final BusinessRole role,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant disabledAt) {
        this.id = Objects.requireNonNull(id, "Business membership id must not be null");
        this.accountId =
                Objects.requireNonNull(accountId, "Membership account id must not be null");
        this.businessId =
                Objects.requireNonNull(businessId, "Membership business id must not be null");
        this.role = Objects.requireNonNull(role, "Membership role must not be null");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "Membership createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Membership updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Membership updatedAt must not be before createdAt");
        }
        if (active && disabledAt != null) {
            throw new IllegalArgumentException("Active membership must not have disabledAt");
        }
        if (!active && disabledAt == null) {
            throw new IllegalArgumentException("Inactive membership must have disabledAt");
        }
        this.disabledAt = disabledAt;
    }

    public static BusinessMembership owner(
            final AccountId accountId, final BusinessId businessId, final Instant now) {
        return new BusinessMembership(
                UUID.randomUUID(), accountId, businessId, BusinessRole.OWNER, true, now, now, null);
    }

    public static BusinessMembership staff(
            final AccountId accountId, final BusinessId businessId, final Instant now) {
        return new BusinessMembership(
                UUID.randomUUID(), accountId, businessId, BusinessRole.STAFF, true, now, now, null);
    }

    public static BusinessMembership reconstitute(
            final UUID id,
            final AccountId accountId,
            final BusinessId businessId,
            final BusinessRole role,
            final boolean active,
            final Instant createdAt) {
        return new BusinessMembership(
                id,
                accountId,
                businessId,
                role,
                active,
                createdAt,
                createdAt,
                active ? null : createdAt);
    }

    public static BusinessMembership reconstitute(
            final UUID id,
            final AccountId accountId,
            final BusinessId businessId,
            final BusinessRole role,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant disabledAt) {
        return new BusinessMembership(
                id, accountId, businessId, role, active, createdAt, updatedAt, disabledAt);
    }

    public BusinessMembership reactivateAsStaff(final Instant now) {
        if (active) {
            throw new IllegalStateException("Active membership cannot be reactivated");
        }
        return new BusinessMembership(
                id, accountId, businessId, BusinessRole.STAFF, true, createdAt, now, null);
    }

    public BusinessMembership changeRole(final BusinessRole newRole, final Instant now) {
        if (!active) {
            throw new IllegalStateException("Inactive membership role cannot be changed");
        }
        return new BusinessMembership(
                id, accountId, businessId, newRole, true, createdAt, now, null);
    }

    public BusinessMembership disable(final Instant now) {
        if (!active) {
            return this;
        }
        return new BusinessMembership(id, accountId, businessId, role, false, createdAt, now, now);
    }

    public UUID id() {
        return id;
    }

    public AccountId accountId() {
        return accountId;
    }

    public BusinessId businessId() {
        return businessId;
    }

    public BusinessRole role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant disabledAt() {
        return disabledAt;
    }
}
