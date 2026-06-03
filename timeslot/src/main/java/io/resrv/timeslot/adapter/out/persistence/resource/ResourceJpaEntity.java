package io.resrv.timeslot.adapter.out.persistence.resource;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceBookingOverrides;
import io.resrv.timeslot.domain.resource.ResourceName;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.SlotDuration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "timeslot", name = "resource")
class ResourceJpaEntity {

    @Id private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ResourceStatus status;

    @Column(name = "slot_duration_minutes")
    private Integer slotDurationMinutes;

    @Column(name = "hold_ttl_minutes")
    private Integer holdTtlMinutes;

    @Column(name = "cancellation_window_minutes")
    private Integer cancellationWindowMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ResourceJpaEntity() {}

    ResourceJpaEntity(
            final UUID id,
            final UUID businessId,
            final String name,
            final String description,
            final ResourceStatus status,
            final Integer slotDurationMinutes,
            final Integer holdTtlMinutes,
            final Integer cancellationWindowMinutes,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.slotDurationMinutes = slotDurationMinutes;
        this.holdTtlMinutes = holdTtlMinutes;
        this.cancellationWindowMinutes = cancellationWindowMinutes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static ResourceJpaEntity fromDomain(final Resource resource) {
        final var overrides = resource.bookingOverrides();
        return new ResourceJpaEntity(
                resource.id().value(),
                resource.businessId().value(),
                resource.name().value(),
                resource.description(),
                resource.status(),
                overrides.slotDuration() == null ? null : overrides.slotDuration().minutes(),
                overrides.holdTtl() == null ? null : overrides.holdTtl().minutes(),
                overrides.cancellationWindow() == null
                        ? null
                        : overrides.cancellationWindow().minutes(),
                resource.createdAt(),
                resource.updatedAt());
    }

    Resource toDomain() {
        return Resource.reconstitute(
                ResourceId.of(id),
                BusinessId.of(businessId),
                new ResourceName(name),
                description,
                status,
                new ResourceBookingOverrides(
                        slotDurationMinutes == null ? null : new SlotDuration(slotDurationMinutes),
                        holdTtlMinutes == null ? null : new HoldTtl(holdTtlMinutes),
                        cancellationWindowMinutes == null
                                ? null
                                : new CancellationWindow(cancellationWindowMinutes)),
                createdAt,
                updatedAt);
    }
}
