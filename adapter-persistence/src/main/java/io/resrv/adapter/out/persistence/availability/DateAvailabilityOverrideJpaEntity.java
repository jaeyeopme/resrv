package io.resrv.adapter.out.persistence.availability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "resource_availability_exception")
class DateAvailabilityOverrideJpaEntity {

    @Id private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean closed;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DateAvailabilityOverrideJpaEntity() {}

    DateAvailabilityOverrideJpaEntity(
            final UUID id,
            final UUID tenantId,
            final UUID resourceId,
            final LocalDate date,
            final boolean closed,
            final LocalTime startTime,
            final LocalTime endTime,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.resourceId = resourceId;
        this.date = date;
        this.closed = closed;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    LocalDate getDate() {
        return date;
    }

    boolean isClosed() {
        return closed;
    }

    LocalTime getStartTime() {
        return startTime;
    }

    LocalTime getEndTime() {
        return endTime;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
