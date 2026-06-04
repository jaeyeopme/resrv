package io.resrv.ticketing.application.purchase;

import io.resrv.ticketing.application.event.out.TicketEventQueryPort;
import io.resrv.ticketing.application.purchase.in.ConfirmTicketPurchaseCommand;
import io.resrv.ticketing.application.purchase.in.TicketPurchaseResult;
import io.resrv.ticketing.application.purchase.out.PurchaseConfirmationIdempotencyPort;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseQueryPort;
import io.resrv.ticketing.application.seat.out.TicketSeatClaimPort;
import io.resrv.ticketing.application.seat.out.TicketSeatQueryPort;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotency;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyStatus;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketPurchaseConfirmationService {

    private final TicketEventQueryPort eventQueryPort;
    private final TicketSeatQueryPort seatQueryPort;
    private final TicketSeatClaimPort seatClaimPort;
    private final TicketPurchaseQueryPort purchaseQueryPort;
    private final PurchaseConfirmationIdempotencyPort idempotencyPort;
    private final Clock clock;

    public TicketPurchaseConfirmationService(
            final TicketEventQueryPort eventQueryPort,
            final TicketSeatQueryPort seatQueryPort,
            final TicketSeatClaimPort seatClaimPort,
            final TicketPurchaseQueryPort purchaseQueryPort,
            final PurchaseConfirmationIdempotencyPort idempotencyPort,
            final Clock clock) {
        this.eventQueryPort = eventQueryPort;
        this.seatQueryPort = seatQueryPort;
        this.seatClaimPort = seatClaimPort;
        this.purchaseQueryPort = purchaseQueryPort;
        this.idempotencyPort = idempotencyPort;
        this.clock = clock;
    }

    @Transactional
    public TicketPurchaseResult confirm(final ConfirmTicketPurchaseCommand command) {
        final var uniqueSeatIds = uniqueSeatIds(command.seatIds());
        final var now = clock.instant();
        final var existingIdempotency =
                idempotencyPort.findForCustomerKey(
                        command.customerAccountId(), command.idempotencyKey());
        if (existingIdempotency.isPresent()) {
            return replay(existingIdempotency.orElseThrow(), command, uniqueSeatIds, now);
        }
        final var idempotency =
                idempotencyPort.createPendingOrFindExisting(
                        PurchaseConfirmationIdempotency.pending(
                                command.idempotencyKey(),
                                command.customerAccountId(),
                                command.ticketEventId(),
                                uniqueSeatIds,
                                now));
        if (idempotency.status() != PurchaseConfirmationIdempotencyStatus.PENDING) {
            return replay(idempotency, command, uniqueSeatIds, now);
        }
        final var event =
                eventQueryPort
                        .findById(command.ticketEventId())
                        .filter(ticketEvent -> ticketEvent.allowsFutureClaims())
                        .orElseThrow(
                                () ->
                                        new TicketPurchaseValidationException(
                                                "Ticket event is not available"));
        if (now.isBefore(event.saleWindow().startAt())
                || !now.isBefore(event.saleWindow().endAt())) {
            throw new TicketPurchaseValidationException("Ticket event is not available for sale");
        }
        final var existing =
                purchaseQueryPort.findCustomerPurchaseForSeatSelection(
                        command.ticketEventId(), command.customerAccountId(), uniqueSeatIds);
        if (existing.isPresent()) {
            final var purchase = existing.orElseThrow();
            idempotencyPort.save(
                    idempotency.complete(
                            PurchaseConfirmationIdempotencyStatus.PURCHASED, purchase.id(), now));
            return TicketPurchaseResult.from(purchase);
        }
        final var seats = seatQueryPort.findAllByIds(uniqueSeatIds);
        if (seats.size() != uniqueSeatIds.size()
                || seats.stream().anyMatch(seat -> !seat.isAvailableFor(event.id()))) {
            return completeUnavailableSeats(idempotency, command, uniqueSeatIds, now);
        }
        final var purchase =
                TicketPurchase.create(event.id(), command.customerAccountId(), uniqueSeatIds, now);
        if (!seatClaimPort.claimAvailableSeats(purchase)) {
            return completeUnavailableSeats(idempotency, command, uniqueSeatIds, now);
        }
        idempotencyPort.save(
                idempotency.complete(
                        PurchaseConfirmationIdempotencyStatus.PURCHASED, purchase.id(), now));
        return TicketPurchaseResult.from(purchase);
    }

    private TicketPurchaseResult replay(
            final PurchaseConfirmationIdempotency idempotency,
            final ConfirmTicketPurchaseCommand command,
            final List<TicketSeatId> uniqueSeatIds,
            final Instant now) {
        if (idempotency.expiredAt(now)) {
            throw TicketPurchaseIdempotencyException.expiredKey();
        }
        if (!idempotency.matches(command.ticketEventId(), uniqueSeatIds)) {
            throw TicketPurchaseIdempotencyException.invalidRetry();
        }
        if (idempotency.status() == PurchaseConfirmationIdempotencyStatus.PURCHASED) {
            return TicketPurchaseResult.from(
                    purchaseQueryPort.findById(idempotency.ticketPurchaseId()).orElseThrow());
        }
        if (idempotency.status() == PurchaseConfirmationIdempotencyStatus.UNAVAILABLE_SEATS) {
            return TicketPurchaseResult.unavailable(
                    command.ticketEventId().value(),
                    command.customerAccountId().value(),
                    seatIdValues(uniqueSeatIds));
        }
        throw new TicketPurchaseValidationException("Purchase confirmation is not complete");
    }

    private TicketPurchaseResult completeUnavailableSeats(
            final PurchaseConfirmationIdempotency idempotency,
            final ConfirmTicketPurchaseCommand command,
            final List<TicketSeatId> uniqueSeatIds,
            final Instant now) {
        idempotencyPort.save(
                idempotency.complete(
                        PurchaseConfirmationIdempotencyStatus.UNAVAILABLE_SEATS, null, now));
        return TicketPurchaseResult.unavailable(
                command.ticketEventId().value(),
                command.customerAccountId().value(),
                seatIdValues(uniqueSeatIds));
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

    private static List<UUID> seatIdValues(final List<TicketSeatId> seatIds) {
        return seatIds.stream().map(TicketSeatId::value).toList();
    }
}
