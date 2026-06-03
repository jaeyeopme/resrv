package io.resrv.ticketing.domain.purchase;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TicketPurchase(
        TicketPurchaseId id,
        TicketEventId ticketEventId,
        AccountId customerAccountId,
        List<TicketSeatId> seatIds,
        Instant confirmedAt) {

    public TicketPurchase {
        Objects.requireNonNull(id, "Ticket purchase id must not be null");
        Objects.requireNonNull(ticketEventId, "Ticket event id must not be null");
        Objects.requireNonNull(customerAccountId, "Customer account id must not be null");
        Objects.requireNonNull(seatIds, "Ticket purchase seat ids must not be null");
        seatIds = List.copyOf(seatIds);
        if (seatIds.isEmpty()) {
            throw new IllegalArgumentException("Ticket purchase must contain at least one seat");
        }
        final Set<TicketSeatId> uniqueSeatIds = new LinkedHashSet<>(seatIds);
        if (uniqueSeatIds.size() != seatIds.size()) {
            throw new IllegalArgumentException("Ticket purchase cannot contain duplicate seats");
        }
        Objects.requireNonNull(confirmedAt, "Confirmed at must not be null");
    }

    public static TicketPurchase create(
            final TicketEventId ticketEventId,
            final AccountId customerAccountId,
            final List<TicketSeatId> seatIds,
            final Instant confirmedAt) {
        return new TicketPurchase(
                TicketPurchaseId.create(), ticketEventId, customerAccountId, seatIds, confirmedAt);
    }

    public static TicketPurchase reconstitute(
            final TicketPurchaseId id,
            final TicketEventId ticketEventId,
            final AccountId customerAccountId,
            final List<TicketSeatId> seatIds,
            final Instant confirmedAt) {
        return new TicketPurchase(id, ticketEventId, customerAccountId, seatIds, confirmedAt);
    }

    public boolean ownsSameSelection(
            final AccountId customerAccountId, final List<TicketSeatId> selectedSeatIds) {
        return this.customerAccountId.equals(customerAccountId)
                && new LinkedHashSet<>(seatIds).equals(new LinkedHashSet<>(selectedSeatIds));
    }
}
