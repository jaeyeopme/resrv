package io.resrv.ticketing.adapter.out.persistence.seat;

import io.resrv.ticketing.domain.seat.TicketSeatStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "ticketing", name = "ticket_seat")
class TicketSeatJpaEntity {

    @Id private UUID id;

    @Column(name = "ticket_event_id", nullable = false)
    private UUID ticketEventId;

    @Column(name = "display_label", nullable = false, length = 200)
    private String displayLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketSeatStatus status;

    @Column(name = "purchased_at")
    private Instant purchasedAt;

    @Column(name = "purchase_id")
    private UUID purchaseId;

    protected TicketSeatJpaEntity() {}

    TicketSeatJpaEntity(
            final UUID id,
            final UUID ticketEventId,
            final String displayLabel,
            final TicketSeatStatus status,
            final Instant purchasedAt,
            final UUID purchaseId) {
        this.id = id;
        this.ticketEventId = ticketEventId;
        this.displayLabel = displayLabel;
        this.status = status;
        this.purchasedAt = purchasedAt;
        this.purchaseId = purchaseId;
    }

    UUID id() {
        return id;
    }

    UUID ticketEventId() {
        return ticketEventId;
    }

    String displayLabel() {
        return displayLabel;
    }

    TicketSeatStatus status() {
        return status;
    }

    Instant purchasedAt() {
        return purchasedAt;
    }

    UUID purchaseId() {
        return purchaseId;
    }
}
