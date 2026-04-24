package io.resrv.adapter.out.persistence.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant")
class TenantJpaEntity {

    @Id private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 63)
    private String slug;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "slot_duration", nullable = false)
    private int slotDuration;

    @Column(name = "hold_ttl", nullable = false)
    private int holdTtl;

    @Column(name = "cancellation_window", nullable = false)
    private int cancellationWindow;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TenantJpaEntity() {}

    TenantJpaEntity(
            final UUID id,
            final String name,
            final String slug,
            final String timezone,
            final int slotDuration,
            final int holdTtl,
            final int cancellationWindow,
            final String status,
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

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getSlug() {
        return slug;
    }

    String getTimezone() {
        return timezone;
    }

    int getSlotDuration() {
        return slotDuration;
    }

    int getHoldTtl() {
        return holdTtl;
    }

    int getCancellationWindow() {
        return cancellationWindow;
    }

    String getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
