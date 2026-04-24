package io.resrv.domain.admin;

import io.resrv.domain.tenant.TenantId;
import java.time.Instant;

public final class Admin {

    private final AdminId id;
    private final TenantId tenantId;
    private final Email email;
    private final String hashedPassword;
    private final AdminRole role;
    private final boolean active;
    private final Instant createdAt;

    private Admin(
            final AdminId id,
            final TenantId tenantId,
            final Email email,
            final String hashedPassword,
            final AdminRole role,
            final boolean active,
            final Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static Admin create(
            final TenantId tenantId,
            final Email email,
            final String hashedPassword,
            final Instant createdAt) {
        if (hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Hashed password must not be blank");
        }
        return new Admin(
                AdminId.create(),
                tenantId,
                email,
                hashedPassword,
                AdminRole.OWNER,
                true,
                createdAt);
    }

    public static Admin reconstitute(
            final AdminId id,
            final TenantId tenantId,
            final Email email,
            final String hashedPassword,
            final AdminRole role,
            final boolean active,
            final Instant createdAt) {
        return new Admin(id, tenantId, email, hashedPassword, role, active, createdAt);
    }

    public AdminId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public Email email() {
        return email;
    }

    public String hashedPassword() {
        return hashedPassword;
    }

    public AdminRole role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof Admin other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
