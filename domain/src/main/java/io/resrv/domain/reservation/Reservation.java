package io.resrv.domain.reservation;

import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class Reservation {

    private final ReservationId id;
    private final TenantId tenantId;
    private final ResourceId resourceId;
    private final CustomerId customerId;
    private final Instant startAt;
    private final Instant endAt;
    private final ReservationStatus status;
    @Nullable private final Instant holdExpiresAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    @Nullable private final Instant confirmedAt;
    @Nullable private final Instant cancelledAt;

    private Reservation(
            final ReservationId id,
            final TenantId tenantId,
            final ResourceId resourceId,
            final CustomerId customerId,
            final Instant startAt,
            final Instant endAt,
            final ReservationStatus status,
            @Nullable final Instant holdExpiresAt,
            final Instant createdAt,
            final Instant updatedAt,
            @Nullable final Instant confirmedAt,
            @Nullable final Instant cancelledAt) {
        validateRange(startAt, endAt);
        this.id = Objects.requireNonNull(id, "Reservation id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Reservation tenant id must not be null");
        this.resourceId =
                Objects.requireNonNull(resourceId, "Reservation resource id must not be null");
        this.customerId =
                Objects.requireNonNull(customerId, "Reservation customer id must not be null");
        this.startAt = Objects.requireNonNull(startAt, "Reservation startAt must not be null");
        this.endAt = Objects.requireNonNull(endAt, "Reservation endAt must not be null");
        this.status = Objects.requireNonNull(status, "Reservation status must not be null");
        this.holdExpiresAt = holdExpiresAt;
        this.createdAt =
                Objects.requireNonNull(createdAt, "Reservation createdAt must not be null");
        this.updatedAt =
                Objects.requireNonNull(updatedAt, "Reservation updatedAt must not be null");
        this.confirmedAt = confirmedAt;
        this.cancelledAt = cancelledAt;
    }

    public static Reservation hold(
            final TenantId tenantId,
            final ResourceId resourceId,
            final CustomerId customerId,
            final Instant startAt,
            final Instant endAt,
            final Instant holdExpiresAt,
            final Instant now) {
        if (!now.isBefore(holdExpiresAt)) {
            throw new IllegalArgumentException("Hold expiry must be after creation time");
        }
        return new Reservation(
                ReservationId.create(),
                tenantId,
                resourceId,
                customerId,
                startAt,
                endAt,
                ReservationStatus.HELD,
                holdExpiresAt,
                now,
                now,
                null,
                null);
    }

    public static Reservation reconstitute(
            final ReservationId id,
            final TenantId tenantId,
            final ResourceId resourceId,
            final CustomerId customerId,
            final Instant startAt,
            final Instant endAt,
            final ReservationStatus status,
            @Nullable final Instant holdExpiresAt,
            final Instant createdAt,
            final Instant updatedAt,
            @Nullable final Instant confirmedAt,
            @Nullable final Instant cancelledAt) {
        return new Reservation(
                id,
                tenantId,
                resourceId,
                customerId,
                startAt,
                endAt,
                status,
                holdExpiresAt,
                createdAt,
                updatedAt,
                confirmedAt,
                cancelledAt);
    }

    public Reservation confirm(final Instant now) {
        if (status == ReservationStatus.CONFIRMED) {
            return this;
        }
        if (status != ReservationStatus.HELD) {
            throw new ReservationInvalidStateException(id, status, "confirmed");
        }
        if (holdExpiresAt == null || !now.isBefore(holdExpiresAt)) {
            throw new ReservationHoldExpiredException(id);
        }
        return new Reservation(
                id,
                tenantId,
                resourceId,
                customerId,
                startAt,
                endAt,
                ReservationStatus.CONFIRMED,
                holdExpiresAt,
                createdAt,
                now,
                now,
                null);
    }

    public Reservation expire(final Instant now) {
        if (status != ReservationStatus.HELD) {
            return this;
        }
        if (holdExpiresAt != null && now.isBefore(holdExpiresAt)) {
            return this;
        }
        return new Reservation(
                id,
                tenantId,
                resourceId,
                customerId,
                startAt,
                endAt,
                ReservationStatus.EXPIRED,
                holdExpiresAt,
                createdAt,
                now,
                confirmedAt,
                null);
    }

    public Reservation cancelByCustomer(final Instant now, final Instant cancellationCutoff) {
        if (status != ReservationStatus.HELD && status != ReservationStatus.CONFIRMED) {
            throw new ReservationInvalidStateException(id, status, "cancelled by customer");
        }
        if (!now.isBefore(cancellationCutoff)) {
            throw new ReservationCancellationClosedException(id);
        }
        return new Reservation(
                id,
                tenantId,
                resourceId,
                customerId,
                startAt,
                endAt,
                ReservationStatus.CUSTOMER_CANCELLED,
                holdExpiresAt,
                createdAt,
                now,
                confirmedAt,
                now);
    }

    public Reservation cancelByAdmin(final Instant now) {
        if (status != ReservationStatus.HELD && status != ReservationStatus.CONFIRMED) {
            throw new ReservationInvalidStateException(id, status, "cancelled by admin");
        }
        return new Reservation(
                id,
                tenantId,
                resourceId,
                customerId,
                startAt,
                endAt,
                ReservationStatus.ADMIN_CANCELLED,
                holdExpiresAt,
                createdAt,
                now,
                confirmedAt,
                now);
    }

    public ReservationId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public ResourceId resourceId() {
        return resourceId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public Instant startAt() {
        return startAt;
    }

    public Instant endAt() {
        return endAt;
    }

    public ReservationStatus status() {
        return status;
    }

    public @Nullable Instant holdExpiresAt() {
        return holdExpiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public @Nullable Instant confirmedAt() {
        return confirmedAt;
    }

    public @Nullable Instant cancelledAt() {
        return cancelledAt;
    }

    private static void validateRange(final Instant startAt, final Instant endAt) {
        Objects.requireNonNull(startAt, "Reservation startAt must not be null");
        Objects.requireNonNull(endAt, "Reservation endAt must not be null");
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Reservation startAt must be before endAt");
        }
    }
}
