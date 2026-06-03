package io.resrv.ticketing.adapter.out.persistence.purchase;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "ticketing", name = "ticket_purchase")
class TicketPurchaseJpaEntity {

    @Id private UUID id;

    @Column(name = "ticket_event_id", nullable = false)
    private UUID ticketEventId;

    @Column(name = "customer_account_id", nullable = false)
    private UUID customerAccountId;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ticketSeatId ASC")
    private List<TicketPurchaseSeatJpaEntity> seats = new ArrayList<>();

    protected TicketPurchaseJpaEntity() {}

    TicketPurchaseJpaEntity(
            final UUID id,
            final UUID ticketEventId,
            final UUID customerAccountId,
            final Instant confirmedAt) {
        this.id = id;
        this.ticketEventId = ticketEventId;
        this.customerAccountId = customerAccountId;
        this.confirmedAt = confirmedAt;
    }

    void replaceSeats(final List<TicketPurchaseSeatJpaEntity> seats) {
        this.seats.clear();
        for (final var seat : seats) {
            seat.attachTo(this);
            this.seats.add(seat);
        }
    }

    UUID id() {
        return id;
    }

    UUID ticketEventId() {
        return ticketEventId;
    }

    UUID customerAccountId() {
        return customerAccountId;
    }

    Instant confirmedAt() {
        return confirmedAt;
    }

    List<TicketPurchaseSeatJpaEntity> seats() {
        return List.copyOf(seats);
    }
}
