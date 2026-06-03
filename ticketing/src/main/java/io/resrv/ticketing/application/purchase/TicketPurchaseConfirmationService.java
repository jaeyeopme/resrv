package io.resrv.ticketing.application.purchase;

import io.resrv.ticketing.application.event.out.TicketEventQueryPort;
import io.resrv.ticketing.application.purchase.in.ConfirmTicketPurchaseCommand;
import io.resrv.ticketing.application.purchase.in.TicketPurchaseResult;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseCommandPort;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseQueryPort;
import io.resrv.ticketing.application.seat.out.TicketSeatCommandPort;
import io.resrv.ticketing.application.seat.out.TicketSeatQueryPort;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
import io.resrv.ticketing.domain.seat.TicketSeat;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketPurchaseConfirmationService {

    private final TicketEventQueryPort eventQueryPort;
    private final TicketSeatQueryPort seatQueryPort;
    private final TicketSeatCommandPort seatCommandPort;
    private final TicketPurchaseCommandPort purchaseCommandPort;
    private final TicketPurchaseQueryPort purchaseQueryPort;
    private final Clock clock;

    public TicketPurchaseConfirmationService(
            final TicketEventQueryPort eventQueryPort,
            final TicketSeatQueryPort seatQueryPort,
            final TicketSeatCommandPort seatCommandPort,
            final TicketPurchaseCommandPort purchaseCommandPort,
            final TicketPurchaseQueryPort purchaseQueryPort,
            final Clock clock) {
        this.eventQueryPort = eventQueryPort;
        this.seatQueryPort = seatQueryPort;
        this.seatCommandPort = seatCommandPort;
        this.purchaseCommandPort = purchaseCommandPort;
        this.purchaseQueryPort = purchaseQueryPort;
        this.clock = clock;
    }

    @Transactional
    public TicketPurchaseResult confirm(final ConfirmTicketPurchaseCommand command) {
        final var uniqueSeatIds = uniqueSeatIds(command.seatIds());
        final var event =
                eventQueryPort
                        .findById(command.ticketEventId())
                        .filter(ticketEvent -> ticketEvent.allowsFutureClaims())
                        .orElseThrow(
                                () ->
                                        new TicketPurchaseValidationException(
                                                "Ticket event is not available"));
        final var now = clock.instant();
        if (now.isBefore(event.saleWindow().startAt())
                || !now.isBefore(event.saleWindow().endAt())) {
            throw new TicketPurchaseValidationException("Ticket event is not available for sale");
        }
        final var existing =
                purchaseQueryPort.findCustomerPurchaseForSeatSelection(
                        command.ticketEventId(), command.customerAccountId(), uniqueSeatIds);
        if (existing.isPresent()) {
            return TicketPurchaseResult.from(existing.orElseThrow());
        }
        final var seats = seatQueryPort.findAllByIds(uniqueSeatIds);
        if (seats.size() != uniqueSeatIds.size()
                || seats.stream().anyMatch(seat -> !seat.isAvailableFor(event.id()))) {
            throw new TicketPurchaseValidationException("Selected seats are unavailable");
        }
        final var purchase =
                TicketPurchase.create(event.id(), command.customerAccountId(), uniqueSeatIds, now);
        final var purchasedSeats =
                seats.stream()
                        .map(seat -> seat.purchase(purchase.id(), now))
                        .sorted((left, right) -> compareBySelection(uniqueSeatIds, left, right))
                        .toList();
        purchaseCommandPort.save(purchase);
        seatCommandPort.saveAll(purchasedSeats);
        return TicketPurchaseResult.from(purchase);
    }

    private static List<TicketSeatId> uniqueSeatIds(final List<TicketSeatId> seatIds) {
        final var unique = new LinkedHashSet<>(seatIds);
        if (unique.isEmpty()) {
            throw new TicketPurchaseValidationException("At least one ticket seat is required");
        }
        if (unique.size() != seatIds.size()) {
            throw new TicketPurchaseValidationException("Duplicate ticket seats are not allowed");
        }
        return List.copyOf(unique);
    }

    private static int compareBySelection(
            final List<TicketSeatId> selectedSeatIds,
            final TicketSeat left,
            final TicketSeat right) {
        return Integer.compare(
                selectedSeatIds.indexOf(left.id()), selectedSeatIds.indexOf(right.id()));
    }
}
