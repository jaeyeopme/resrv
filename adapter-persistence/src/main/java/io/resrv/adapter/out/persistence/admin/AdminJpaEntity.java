package io.resrv.adapter.out.persistence.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin")
class AdminJpaEntity {

    @Id private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // JPA proxy requirement
    protected AdminJpaEntity() {}

    AdminJpaEntity(
            final UUID id,
            final UUID tenantId,
            final String email,
            final String hashedPassword,
            final String role,
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

    UUID getId() {
        return id;
    }

    UUID getTenantId() {
        return tenantId;
    }

    String getEmail() {
        return email;
    }

    String getHashedPassword() {
        return hashedPassword;
    }

    String getRole() {
        return role;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
