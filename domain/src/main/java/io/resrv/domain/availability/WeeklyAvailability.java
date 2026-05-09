package io.resrv.domain.availability;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

public final class WeeklyAvailability {

    private final WeeklyAvailabilityId id;
    private final TenantId tenantId;
    private final ResourceId resourceId;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Instant createdAt;
    private final Instant updatedAt;

    private WeeklyAvailability(
            final WeeklyAvailabilityId id,
            final TenantId tenantId,
            final ResourceId resourceId,
            final DayOfWeek dayOfWeek,
            final LocalTime startTime,
            final LocalTime endTime,
            final Instant createdAt,
            final Instant updatedAt) {
        validateRange(startTime, endTime);
        this.id = Objects.requireNonNull(id, "Weekly availability id must not be null");
        this.tenantId =
                Objects.requireNonNull(tenantId, "Weekly availability tenant id must not be null");
        this.resourceId =
                Objects.requireNonNull(
                        resourceId, "Weekly availability resource id must not be null");
        this.dayOfWeek =
                Objects.requireNonNull(dayOfWeek, "Weekly availability day must not be null");
        this.startTime =
                Objects.requireNonNull(
                        startTime, "Weekly availability start time must not be null");
        this.endTime =
                Objects.requireNonNull(endTime, "Weekly availability end time must not be null");
        this.createdAt =
                Objects.requireNonNull(createdAt, "Weekly availability createdAt must not be null");
        this.updatedAt =
                Objects.requireNonNull(updatedAt, "Weekly availability updatedAt must not be null");
    }

    public static WeeklyAvailability create(
            final TenantId tenantId,
            final ResourceId resourceId,
            final DayOfWeek dayOfWeek,
            final LocalTime startTime,
            final LocalTime endTime,
            final Instant now) {
        return new WeeklyAvailability(
                WeeklyAvailabilityId.create(),
                tenantId,
                resourceId,
                dayOfWeek,
                startTime,
                endTime,
                now,
                now);
    }

    public static WeeklyAvailability reconstitute(
            final WeeklyAvailabilityId id,
            final TenantId tenantId,
            final ResourceId resourceId,
            final DayOfWeek dayOfWeek,
            final LocalTime startTime,
            final LocalTime endTime,
            final Instant createdAt,
            final Instant updatedAt) {
        return new WeeklyAvailability(
                id, tenantId, resourceId, dayOfWeek, startTime, endTime, createdAt, updatedAt);
    }

    public WeeklyAvailability update(
            final LocalTime newStartTime, final LocalTime newEndTime, final Instant now) {
        return new WeeklyAvailability(
                id, tenantId, resourceId, dayOfWeek, newStartTime, newEndTime, createdAt, now);
    }

    public WeeklyAvailabilityId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public ResourceId resourceId() {
        return resourceId;
    }

    public DayOfWeek dayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime startTime() {
        return startTime;
    }

    public LocalTime endTime() {
        return endTime;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static void validateRange(final LocalTime startTime, final LocalTime endTime) {
        Objects.requireNonNull(startTime, "Availability start time must not be null");
        Objects.requireNonNull(endTime, "Availability end time must not be null");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Availability start time must be before end time");
        }
    }
}
