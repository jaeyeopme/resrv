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

    private BusinessMembership(
            final UUID id,
            final AccountId accountId,
            final BusinessId businessId,
            final BusinessRole role,
            final boolean active,
            final Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Business membership id must not be null");
        this.accountId =
                Objects.requireNonNull(accountId, "Membership account id must not be null");
        this.businessId =
                Objects.requireNonNull(businessId, "Membership business id must not be null");
        this.role = Objects.requireNonNull(role, "Membership role must not be null");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "Membership createdAt must not be null");
    }

    public static BusinessMembership owner(
            final AccountId accountId, final BusinessId businessId, final Instant now) {
        return new BusinessMembership(
                UUID.randomUUID(), accountId, businessId, BusinessRole.OWNER, true, now);
    }

    public static BusinessMembership reconstitute(
            final UUID id,
            final AccountId accountId,
            final BusinessId businessId,
            final BusinessRole role,
            final boolean active,
            final Instant createdAt) {
        return new BusinessMembership(id, accountId, businessId, role, active, createdAt);
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
}
