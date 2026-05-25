package io.resrv.timeslot.adapter.out.persistence.settings;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "timeslot", name = "business_booking_settings")
class BusinessBookingSettingsJpaEntity {

    @Id
    @Column(name = "business_id")
    private UUID businessId;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Column(name = "hold_ttl_minutes", nullable = false)
    private int holdTtlMinutes;

    @Column(name = "cancellation_window_minutes", nullable = false)
    private int cancellationWindowMinutes;

    @Column(name = "max_advance_booking_days", nullable = false)
    private int maxAdvanceBookingDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessBookingSettingsJpaEntity() {}

    BusinessBookingSettingsJpaEntity(
            final UUID businessId,
            final int slotDurationMinutes,
            final int holdTtlMinutes,
            final int cancellationWindowMinutes,
            final int maxAdvanceBookingDays,
            final Instant createdAt,
            final Instant updatedAt) {
        this.businessId = businessId;
        this.slotDurationMinutes = slotDurationMinutes;
        this.holdTtlMinutes = holdTtlMinutes;
        this.cancellationWindowMinutes = cancellationWindowMinutes;
        this.maxAdvanceBookingDays = maxAdvanceBookingDays;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static BusinessBookingSettingsJpaEntity fromDomain(final BusinessBookingSettings settings) {
        return new BusinessBookingSettingsJpaEntity(
                settings.businessId().value(),
                settings.slotDuration().minutes(),
                settings.holdTtl().minutes(),
                settings.cancellationWindow().minutes(),
                settings.maxAdvanceBookingDays().days(),
                settings.createdAt(),
                settings.updatedAt());
    }

    BusinessBookingSettings toDomain() {
        return BusinessBookingSettings.reconstitute(
                BusinessId.of(businessId),
                new SlotDuration(slotDurationMinutes),
                new HoldTtl(holdTtlMinutes),
                new CancellationWindow(cancellationWindowMinutes),
                new MaxAdvanceBookingDays(maxAdvanceBookingDays),
                createdAt,
                updatedAt);
    }
}
