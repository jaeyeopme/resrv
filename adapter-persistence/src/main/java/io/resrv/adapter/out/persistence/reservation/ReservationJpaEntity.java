package io.resrv.adapter.out.persistence.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation")
class ReservationJpaEntity {

    @Id private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected ReservationJpaEntity() {}

    ReservationJpaEntity(
            final UUID id,
            final UUID tenantId,
            final UUID resourceId,
            final UUID customerId,
            final Instant startAt,
            final Instant endAt,
            final String status,
            final Instant holdExpiresAt,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant confirmedAt,
            final Instant cancelledAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.resourceId = resourceId;
        this.customerId = customerId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.holdExpiresAt = holdExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.confirmedAt = confirmedAt;
        this.cancelledAt = cancelledAt;
    }

    UUID getId() {
        return id;
    }

    UUID getTenantId() {
        return tenantId;
    }

    UUID getResourceId() {
        return resourceId;
    }

    UUID getCustomerId() {
        return customerId;
    }

    Instant getStartAt() {
        return startAt;
    }

    Instant getEndAt() {
        return endAt;
    }

    String getStatus() {
        return status;
    }

    Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Instant getConfirmedAt() {
        return confirmedAt;
    }

    Instant getCancelledAt() {
        return cancelledAt;
    }
}
