package io.resrv.domain.customer;

import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import java.util.Objects;

public final class Customer {

    private final CustomerId id;
    private final TenantId tenantId;
    private final CustomerEmail email;
    private final CustomerName name;
    private final String hashedPassword;
    private final boolean active;
    private final Instant createdAt;

    private Customer(
            final CustomerId id,
            final TenantId tenantId,
            final CustomerEmail email,
            final CustomerName name,
            final String hashedPassword,
            final boolean active,
            final Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Customer id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Customer tenant id must not be null");
        this.email = Objects.requireNonNull(email, "Customer email must not be null");
        this.name = Objects.requireNonNull(name, "Customer name must not be null");
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Customer hashed password must not be blank");
        }
        this.hashedPassword = hashedPassword;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "Customer createdAt must not be null");
    }

    public static Customer create(
            final TenantId tenantId,
            final CustomerEmail email,
            final CustomerName name,
            final String hashedPassword,
            final Instant createdAt) {
        return new Customer(
                CustomerId.create(), tenantId, email, name, hashedPassword, true, createdAt);
    }

    public static Customer reconstitute(
            final CustomerId id,
            final TenantId tenantId,
            final CustomerEmail email,
            final CustomerName name,
            final String hashedPassword,
            final boolean active,
            final Instant createdAt) {
        return new Customer(id, tenantId, email, name, hashedPassword, active, createdAt);
    }

    public CustomerId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public CustomerEmail email() {
        return email;
    }

    public CustomerName name() {
        return name;
    }

    public String hashedPassword() {
        return hashedPassword;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof Customer other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
