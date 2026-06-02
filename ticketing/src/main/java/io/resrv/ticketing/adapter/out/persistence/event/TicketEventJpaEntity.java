package io.resrv.ticketing.adapter.out.persistence.event;

import io.resrv.ticketing.domain.event.TicketEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "ticketing", name = "ticket_event")
class TicketEventJpaEntity {

    @Id private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "event_start_at", nullable = false)
    private Instant eventStartAt;

    @Column(name = "event_end_at", nullable = false)
    private Instant eventEndAt;

    @Column(name = "event_timezone", nullable = false, length = 64)
    private String eventTimezone;

    @Column(name = "sale_start_at", nullable = false)
    private Instant saleStartAt;

    @Column(name = "sale_end_at", nullable = false)
    private Instant saleEndAt;

    @Column(name = "sale_timezone", nullable = false, length = 64)
    private String saleTimezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketEventStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TicketEventJpaEntity() {}

    TicketEventJpaEntity(
            final UUID id,
            final UUID businessId,
            final String title,
            final Instant eventStartAt,
            final Instant eventEndAt,
            final String eventTimezone,
            final Instant saleStartAt,
            final Instant saleEndAt,
            final String saleTimezone,
            final TicketEventStatus status,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.businessId = businessId;
        this.title = title;
        this.eventStartAt = eventStartAt;
        this.eventEndAt = eventEndAt;
        this.eventTimezone = eventTimezone;
        this.saleStartAt = saleStartAt;
        this.saleEndAt = saleEndAt;
        this.saleTimezone = saleTimezone;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID id() {
        return id;
    }

    UUID businessId() {
        return businessId;
    }

    String title() {
        return title;
    }

    Instant eventStartAt() {
        return eventStartAt;
    }

    Instant eventEndAt() {
        return eventEndAt;
    }

    String eventTimezone() {
        return eventTimezone;
    }

    Instant saleStartAt() {
        return saleStartAt;
    }

    Instant saleEndAt() {
        return saleEndAt;
    }

    String saleTimezone() {
        return saleTimezone;
    }

    TicketEventStatus status() {
        return status;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
