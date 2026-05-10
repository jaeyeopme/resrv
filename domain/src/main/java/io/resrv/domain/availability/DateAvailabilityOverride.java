package io.resrv.domain.availability;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public final class DateAvailabilityOverride {

    private final DateAvailabilityOverrideId id;
    private final TenantId tenantId;
    private final ResourceId resourceId;
    private final LocalDate date;
    private final boolean closed;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Instant createdAt;
    private final Instant updatedAt;

    private DateAvailabilityOverride(
            final DateAvailabilityOverrideId id,
            final TenantId tenantId,
            final ResourceId resourceId,
            final LocalDate date,
            final boolean closed,
            final LocalTime startTime,
            final LocalTime endTime,
            final Instant createdAt,
            final Instant updatedAt) {
        validateRange(closed, startTime, endTime);
        this.id = Objects.requireNonNull(id, "Date availability override id must not be null");
        this.tenantId =
                Objects.requireNonNull(tenantId, "Date override tenant id must not be null");
        this.resourceId =
                Objects.requireNonNull(resourceId, "Date override resource id must not be null");
        this.date = Objects.requireNonNull(date, "Date override date must not be null");
        this.closed = closed;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt =
                Objects.requireNonNull(createdAt, "Date override createdAt must not be null");
        this.updatedAt =
                Objects.requireNonNull(updatedAt, "Date override updatedAt must not be null");
    }

    public static DateAvailabilityOverride closed(
            final TenantId tenantId,
            final ResourceId resourceId,
            final LocalDate date,
            final Instant now) {
        return new DateAvailabilityOverride(
                DateAvailabilityOverrideId.create(),
                tenantId,
                resourceId,
                date,
                true,
                null,
                null,
                now,
                now);
    }

    public boolean closed() {
        return closed;
    }

    public static DateAvailabilityOverride open(
            final TenantId tenantId,
            final ResourceId resourceId,
            final LocalDate date,
            final LocalTime startTime,
            final LocalTime endTime,
            final Instant now) {
        return new DateAvailabilityOverride(
                DateAvailabilityOverrideId.create(),
                tenantId,
                resourceId,
                date,
                false,
                startTime,
                endTime,
                now,
                now);
    }

    public static DateAvailabilityOverride reconstitute(
            final DateAvailabilityOverrideId id,
            final TenantId tenantId,
            final ResourceId resourceId,
            final LocalDate date,
            final boolean closed,
            final LocalTime startTime,
            final LocalTime endTime,
            final Instant createdAt,
            final Instant updatedAt) {
        return new DateAvailabilityOverride(
                id, tenantId, resourceId, date, closed, startTime, endTime, createdAt, updatedAt);
    }

    public DateAvailabilityOverride updateClosed(final Instant now) {
        return new DateAvailabilityOverride(
                id, tenantId, resourceId, date, true, null, null, createdAt, now);
    }

    public DateAvailabilityOverride updateOpen(
            final LocalTime newStartTime, final LocalTime newEndTime, final Instant now) {
        return new DateAvailabilityOverride(
                id, tenantId, resourceId, date, false, newStartTime, newEndTime, createdAt, now);
    }

    public DateAvailabilityOverrideId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public ResourceId resourceId() {
        return resourceId;
    }

    public LocalDate date() {
        return date;
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

    private static void validateRange(
            final boolean closed, final LocalTime startTime, final LocalTime endTime) {
        if (closed) {
            return;
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Open date override requires start and end time");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Date override start time must be before end time");
        }
    }
}
