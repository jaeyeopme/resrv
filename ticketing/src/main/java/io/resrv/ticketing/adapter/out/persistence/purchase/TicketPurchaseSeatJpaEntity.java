package io.resrv.ticketing.adapter.out.persistence.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(schema = "ticketing", name = "ticket_purchase_seat")
@IdClass(TicketPurchaseSeatJpaEntity.Key.class)
class TicketPurchaseSeatJpaEntity {

    @Id
    @Column(name = "ticket_purchase_id", nullable = false)
    private UUID ticketPurchaseId;

    @Id
    @Column(name = "ticket_seat_id", nullable = false)
    private UUID ticketSeatId;

    @ManyToOne
    @JoinColumn(name = "ticket_purchase_id", insertable = false, updatable = false)
    private TicketPurchaseJpaEntity purchase;

    protected TicketPurchaseSeatJpaEntity() {}

    TicketPurchaseSeatJpaEntity(final UUID ticketSeatId) {
        this.ticketSeatId = ticketSeatId;
    }

    void attachTo(final TicketPurchaseJpaEntity purchase) {
        this.purchase = purchase;
        this.ticketPurchaseId = purchase.id();
    }

    UUID ticketSeatId() {
        return ticketSeatId;
    }

    record Key(UUID ticketPurchaseId, UUID ticketSeatId) implements Serializable {}
}
