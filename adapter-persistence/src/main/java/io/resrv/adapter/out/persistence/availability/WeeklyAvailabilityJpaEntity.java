package io.resrv.adapter.out.persistence.availability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "resource_weekly_availability")
class WeeklyAvailabilityJpaEntity {

    @Id private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WeeklyAvailabilityJpaEntity() {}

    WeeklyAvailabilityJpaEntity(
            final UUID id,
            final UUID tenantId,
            final UUID resourceId,
            final short dayOfWeek,
            final LocalTime startTime,
            final LocalTime endTime,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.resourceId = resourceId;
        this.dayOfWeek = dayOfWeek;
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

    short getDayOfWeek() {
        return dayOfWeek;
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
