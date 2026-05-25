package io.resrv.timeslot.adapter.out.persistence.reservation;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.reservation.Reservation;
import io.resrv.timeslot.domain.reservation.ReservationCancellationActor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "timeslot", name = "reservation")
class ReservationJpaEntity {

    @Id private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "customer_account_id", nullable = false)
    private UUID customerAccountId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "hold_expires_at", nullable = false)
    private Instant holdExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 32)
    private ReservationCancellationActor cancelledBy;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "no_show_at")
    private Instant noShowAt;

    protected ReservationJpaEntity() {}

    ReservationJpaEntity(
            final UUID id,
            final UUID businessId,
            final UUID resourceId,
            final UUID customerAccountId,
            final Instant startAt,
            final Instant endAt,
            final Instant holdExpiresAt,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant confirmedAt,
            final Instant releasedAt,
            final Instant cancelledAt,
            final ReservationCancellationActor cancelledBy,
            final Instant checkedInAt,
            final Instant noShowAt) {
        this.id = id;
        this.businessId = businessId;
        this.resourceId = resourceId;
        this.customerAccountId = customerAccountId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.holdExpiresAt = holdExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.confirmedAt = confirmedAt;
        this.releasedAt = releasedAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.checkedInAt = checkedInAt;
        this.noShowAt = noShowAt;
    }

    static ReservationJpaEntity fromDomain(final Reservation reservation) {
        return new ReservationJpaEntity(
                reservation.id().value(),
                reservation.businessId().value(),
                reservation.resourceId().value(),
                reservation.customerAccountId().value(),
                reservation.startAt(),
                reservation.endAt(),
                reservation.holdExpiresAt(),
                reservation.createdAt(),
                reservation.updatedAt(),
                reservation.confirmedAt(),
                reservation.releasedAt(),
                reservation.cancelledAt(),
                reservation.cancelledBy(),
                reservation.checkedInAt(),
                reservation.noShowAt());
    }

    Reservation toDomain() {
        return Reservation.reconstitute(
                ReservationId.of(id),
                BusinessId.of(businessId),
                ResourceId.of(resourceId),
                AccountId.of(customerAccountId),
                startAt,
                endAt,
                holdExpiresAt,
                createdAt,
                updatedAt,
                confirmedAt,
                releasedAt,
                cancelledAt,
                cancelledBy,
                checkedInAt,
                noShowAt);
    }
}
