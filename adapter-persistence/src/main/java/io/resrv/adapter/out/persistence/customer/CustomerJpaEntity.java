package io.resrv.adapter.out.persistence.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer")
class CustomerJpaEntity {

    @Id private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CustomerJpaEntity() {}

    CustomerJpaEntity(
            final UUID id,
            final UUID tenantId,
            final String email,
            final String name,
            final String hashedPassword,
            final boolean active,
            final Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.name = name;
        this.hashedPassword = hashedPassword;
        this.active = active;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getTenantId() {
        return tenantId;
    }

    String getEmail() {
        return email;
    }

    String getName() {
        return name;
    }

    String getHashedPassword() {
        return hashedPassword;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
