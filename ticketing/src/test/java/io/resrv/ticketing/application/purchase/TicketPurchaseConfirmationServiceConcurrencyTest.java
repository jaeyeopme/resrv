package io.resrv.ticketing.application.purchase;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.application.purchase.in.ConfirmTicketPurchaseCommand;
import io.resrv.ticketing.application.purchase.in.TicketPurchaseResult;
import io.resrv.ticketing.application.purchase.out.PurchaseConfirmationIdempotencyPort;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseQueryPort;
import io.resrv.ticketing.application.seat.out.TicketSeatClaimPort;
import io.resrv.ticketing.application.seat.out.TicketSeatQueryPort;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotency;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyKey;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
import io.resrv.ticketing.domain.purchase.TicketPurchaseId;
import io.resrv.ticketing.domain.seat.TicketSeat;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import io.resrv.ticketing.support.TicketingTestFixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

final class TicketPurchaseConfirmationServiceConcurrencyTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Test
    void sameSeatRaceProducesOnePurchaseAndUnavailableLosers() throws Exception {
        final var event =
                TicketingTestFixtures.event("Concert", Instant.parse("2026-06-03T01:00:00Z"), NOW);
        final var seat = TicketSeat.createAvailable(event.id(), "A-1");
        final var state = new State(event.id(), seat);
        final var service =
                new TicketPurchaseConfirmationService(
                        eventId -> Optional.of(event),
                        state,
                        state,
                        state,
                        state,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        try (final var executor = Executors.newFixedThreadPool(8)) {
            final var futures =
                    IntStream.range(0, 50)
                            .mapToObj(
                                    index ->
                                            executor.submit(
                                                    () ->
                                                            service.confirm(
                                                                    new ConfirmTicketPurchaseCommand(
                                                                            event.id(),
                                                                            AccountId.create(),
                                                                            List.of(seat.id()),
                                                                            PurchaseConfirmationIdempotencyKey
                                                                                    .of(
                                                                                            "key-"
                                                                                                    + index)))))
                            .toList();
            final var results = new ArrayList<TicketPurchaseResult>();
            for (final var future : futures) {
                results.add(future.get());
            }

            assertThat(results.stream().filter(result -> result.purchased()).count()).isEqualTo(1);
            assertThat(results.stream().filter(result -> !result.purchased()).count())
                    .isEqualTo(49);
            assertThat(state.purchases).hasSize(1);
            assertThat(state.ownedSeat.purchaseId()).isEqualTo(state.purchases.getFirst().id());
        }
    }

    private static final class State
            implements TicketSeatQueryPort,
                    TicketSeatClaimPort,
                    TicketPurchaseQueryPort,
                    PurchaseConfirmationIdempotencyPort {

        final TicketEventId eventId;
        TicketSeat ownedSeat;
        final List<TicketPurchase> purchases = new ArrayList<>();
        final List<PurchaseConfirmationIdempotency> idempotencies = new ArrayList<>();

        State(final TicketEventId eventId, final TicketSeat seat) {
            this.eventId = eventId;
            this.ownedSeat = seat;
        }

        @Override
        public synchronized Optional<TicketSeat> findById(final TicketSeatId ticketSeatId) {
            return Optional.of(ownedSeat).filter(seat -> seat.id().equals(ticketSeatId));
        }

        @Override
        public synchronized Optional<TicketPurchase> findById(
                final TicketPurchaseId ticketPurchaseId) {
            return purchases.stream()
                    .filter(purchase -> purchase.id().equals(ticketPurchaseId))
                    .findFirst();
        }

        @Override
        public synchronized List<TicketSeat> findAllByIds(final List<TicketSeatId> ticketSeatIds) {
            return ticketSeatIds.contains(ownedSeat.id()) ? List.of(ownedSeat) : List.of();
        }

        @Override
        public synchronized List<TicketSeat> findAllByEventId(final TicketEventId ticketEventId) {
            return eventId.equals(ticketEventId) ? List.of(ownedSeat) : List.of();
        }

        @Override
        public synchronized boolean claimAvailableSeats(final TicketPurchase purchase) {
            if (!ownedSeat.isAvailableFor(purchase.ticketEventId())) {
                return false;
            }
            ownedSeat = ownedSeat.purchase(purchase.id(), purchase.confirmedAt());
            purchases.add(purchase);
            return true;
        }

        @Override
        public synchronized Optional<TicketPurchase> findCustomerPurchaseForSeatSelection(
                final TicketEventId ticketEventId,
                final AccountId customerAccountId,
                final List<TicketSeatId> seatIds) {
            return purchases.stream()
                    .filter(purchase -> purchase.ownsSameSelection(customerAccountId, seatIds))
                    .findFirst();
        }

        @Override
        public synchronized Optional<PurchaseConfirmationIdempotency> findForCustomerKey(
                final AccountId customerAccountId,
                final PurchaseConfirmationIdempotencyKey idempotencyKey) {
            return idempotencies.stream()
                    .filter(
                            idempotency ->
                                    idempotency.customerAccountId().equals(customerAccountId)
                                            && idempotency.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public synchronized PurchaseConfirmationIdempotency save(
                final PurchaseConfirmationIdempotency idempotency) {
            idempotencies.removeIf(
                    existing ->
                            existing.customerAccountId().equals(idempotency.customerAccountId())
                                    && existing.idempotencyKey()
                                            .equals(idempotency.idempotencyKey()));
            idempotencies.add(idempotency);
            return idempotency;
        }

        @Override
        public synchronized PurchaseConfirmationIdempotency createPendingOrFindExisting(
                final PurchaseConfirmationIdempotency idempotency) {
            final var existing =
                    findForCustomerKey(
                            idempotency.customerAccountId(), idempotency.idempotencyKey());
            return existing.orElseGet(() -> save(idempotency));
        }
    }
}
