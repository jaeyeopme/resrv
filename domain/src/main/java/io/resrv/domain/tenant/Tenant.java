package io.resrv.domain.tenant;

import java.time.Instant;

public final class Tenant {

    private final TenantId id;
    private final TenantName name;
    private final Slug slug;
    private final Timezone timezone;
    private final SlotDuration slotDuration;
    private final HoldTtl holdTtl;
    private final CancellationWindow cancellationWindow;
    private final TenantStatus status;
    private final Instant createdAt;

    private Tenant(
            final TenantId id,
            final TenantName name,
            final Slug slug,
            final Timezone timezone,
            final SlotDuration slotDuration,
            final HoldTtl holdTtl,
            final CancellationWindow cancellationWindow,
            final TenantStatus status,
            final Instant createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.timezone = timezone;
        this.slotDuration = slotDuration;
        this.holdTtl = holdTtl;
        this.cancellationWindow = cancellationWindow;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Tenant create(
            final TenantName name,
            final Slug slug,
            final Timezone timezone,
            final SlotDuration slotDuration,
            final HoldTtl holdTtl,
            final CancellationWindow cancellationWindow,
            final Instant createdAt) {
        return new Tenant(
                TenantId.create(),
                name,
                slug,
                timezone,
                slotDuration,
                holdTtl,
                cancellationWindow,
                TenantStatus.PENDING,
                createdAt);
    }

    public static Tenant reconstitute(
            final TenantId id,
            final TenantName name,
            final Slug slug,
            final Timezone timezone,
            final SlotDuration slotDuration,
            final HoldTtl holdTtl,
            final CancellationWindow cancellationWindow,
            final TenantStatus status,
            final Instant createdAt) {
        return new Tenant(
                id,
                name,
                slug,
                timezone,
                slotDuration,
                holdTtl,
                cancellationWindow,
                status,
                createdAt);
    }

    public TenantId id() {
        return id;
    }

    public TenantName name() {
        return name;
    }

    public Slug slug() {
        return slug;
    }

    public Timezone timezone() {
        return timezone;
    }

    public SlotDuration slotDuration() {
        return slotDuration;
    }

    public HoldTtl holdTtl() {
        return holdTtl;
    }

    public CancellationWindow cancellationWindow() {
        return cancellationWindow;
    }

    public TenantStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof Tenant other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
