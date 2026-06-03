package io.resrv.ticketing.domain.seat;

import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.TicketPurchaseId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record TicketSeat(
        TicketSeatId id,
        TicketEventId ticketEventId,
        String displayLabel,
        TicketSeatStatus status,
        Instant purchasedAt,
        TicketPurchaseId purchaseId) {

    public TicketSeat {
        Objects.requireNonNull(id, "Ticket seat id must not be null");
        Objects.requireNonNull(ticketEventId, "Ticket event id must not be null");
        Objects.requireNonNull(displayLabel, "Ticket seat display label must not be null");
        if (displayLabel.isBlank()) {
            throw new IllegalArgumentException("Ticket seat display label must not be blank");
        }
        Objects.requireNonNull(status, "Ticket seat status must not be null");
        if (status == TicketSeatStatus.AVAILABLE && (purchasedAt != null || purchaseId != null)) {
            throw new IllegalArgumentException("Available ticket seats cannot have purchase state");
        }
        if (status == TicketSeatStatus.PURCHASED && (purchasedAt == null || purchaseId == null)) {
            throw new IllegalArgumentException("Purchased ticket seats require purchase state");
        }
    }

    public static TicketSeat createAvailable(
            final TicketEventId ticketEventId, final String displayLabel) {
        return new TicketSeat(
                TicketSeatId.create(),
                ticketEventId,
                displayLabel,
                TicketSeatStatus.AVAILABLE,
                null,
                null);
    }

    public static TicketSeat reconstitute(
            final TicketSeatId id,
            final TicketEventId ticketEventId,
            final String displayLabel,
            final TicketSeatStatus status,
            final Instant purchasedAt,
            final TicketPurchaseId purchaseId) {
        return new TicketSeat(id, ticketEventId, displayLabel, status, purchasedAt, purchaseId);
    }

    public Optional<TicketPurchaseId> purchasedBy() {
        return Optional.ofNullable(purchaseId);
    }

    public boolean isAvailableFor(final TicketEventId eventId) {
        return ticketEventId.equals(eventId) && status == TicketSeatStatus.AVAILABLE;
    }

    public TicketSeat purchase(final TicketPurchaseId purchaseId, final Instant purchasedAt) {
        Objects.requireNonNull(purchaseId, "Ticket purchase id must not be null");
        Objects.requireNonNull(purchasedAt, "Purchased at must not be null");
        if (status != TicketSeatStatus.AVAILABLE) {
            throw new IllegalArgumentException("Ticket seat is not available");
        }
        return new TicketSeat(
                id,
                ticketEventId,
                displayLabel,
                TicketSeatStatus.PURCHASED,
                purchasedAt,
                purchaseId);
    }
}
