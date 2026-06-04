package io.resrv.ticketing.application.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.application.purchase.in.ConfirmTicketPurchaseCommand;
import io.resrv.ticketing.application.purchase.out.PurchaseConfirmationIdempotencyPort;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseQueryPort;
import io.resrv.ticketing.application.seat.out.TicketSeatClaimPort;
import io.resrv.ticketing.application.seat.out.TicketSeatQueryPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotency;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyKey;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyStatus;
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
import org.junit.jupiter.api.Test;

final class TicketPurchaseConfirmationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Test
    void confirmsPurchaseAndMarksAllSeatsPurchased() {
        final var fixture = Fixture.create();
        final var seatIds = fixture.seats.stream().map(TicketSeat::id).toList();

        final var result =
                fixture.service.confirm(
                        new ConfirmTicketPurchaseCommand(
                                fixture.event.id(),
                                fixture.customerId,
                                seatIds,
                                PurchaseConfirmationIdempotencyKey.of("purchase-key")));

        assertThat(result.seatIds())
                .containsExactlyElementsOf(seatIds.stream().map(TicketSeatId::value).toList());
        assertThat(fixture.savedPurchase).isNotNull();
        assertThat(fixture.seats).allSatisfy(seat -> assertThat(seat.purchaseId()).isNotNull());
    }

    @Test
    void rejectsUnavailableSeatsWithoutSavingAttempt() {
        final var fixture = Fixture.create();
        final var soldSeat =
                fixture.seats.getFirst().purchase(TicketPurchaseId.create(), NOW.minusSeconds(60));
        fixture.seats.set(0, soldSeat);

        final var result =
                fixture.service.confirm(
                        new ConfirmTicketPurchaseCommand(
                                fixture.event.id(),
                                fixture.customerId,
                                fixture.seats.stream().map(TicketSeat::id).toList(),
                                PurchaseConfirmationIdempotencyKey.of("purchase-key")));

        assertThat(result.purchased()).isFalse();
        assertThat(fixture.savedPurchase).isNull();
        assertThat(fixture.savedIdempotency.status())
                .isEqualTo(PurchaseConfirmationIdempotencyStatus.UNAVAILABLE_SEATS);
        assertThat(fixture.savedIdempotency.completedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsPurchaseBeforeSaleWindowOpens() {
        final var fixture =
                Fixture.create(Clock.fixed(Instant.parse("2026-05-31T23:59:59Z"), ZoneOffset.UTC));

        assertConfirmRejected(fixture, "Ticket event is not available for sale");
    }

    @Test
    void rejectsPurchaseAfterSaleWindowCloses() {
        final var fixture =
                Fixture.create(Clock.fixed(Instant.parse("2026-06-03T01:00:00Z"), ZoneOffset.UTC));

        assertConfirmRejected(fixture, "Ticket event is not available for sale");
    }

    private static void assertConfirmRejected(final Fixture fixture, final String message) {
        assertThatThrownBy(
                        () ->
                                fixture.service.confirm(
                                        new ConfirmTicketPurchaseCommand(
                                                fixture.event.id(),
                                                fixture.customerId,
                                                fixture.seats.stream().map(TicketSeat::id).toList(),
                                                PurchaseConfirmationIdempotencyKey.of(
                                                        "purchase-key"))))
                .isInstanceOf(TicketPurchaseValidationException.class)
                .hasMessage(message);
        assertThat(fixture.savedPurchase).isNull();
    }

    private static final class Fixture {

        final TicketEvent event = event();
        final AccountId customerId = AccountId.create();
        PurchaseConfirmationIdempotency savedIdempotency;
        final List<TicketSeat> seats =
                new ArrayList<>(
                        List.of(
                                TicketSeat.createAvailable(event.id(), "A-1"),
                                TicketSeat.createAvailable(event.id(), "A-2")));
        TicketPurchase savedPurchase;
        TicketPurchaseConfirmationService service;

        static Fixture create() {
            return create(Clock.fixed(NOW, ZoneOffset.UTC));
        }

        static Fixture create(final Clock clock) {
            final var fixture = new Fixture();
            fixture.service =
                    new TicketPurchaseConfirmationService(
                            eventId -> Optional.of(fixture.event),
                            new TicketSeatQueryPort() {
                                @Override
                                public Optional<TicketSeat> findById(
                                        final TicketSeatId ticketSeatId) {
                                    return fixture.seats.stream()
                                            .filter(seat -> seat.id().equals(ticketSeatId))
                                            .findFirst();
                                }

                                @Override
                                public List<TicketSeat> findAllByIds(
                                        final List<TicketSeatId> ticketSeatIds) {
                                    return fixture.seats.stream()
                                            .filter(seat -> ticketSeatIds.contains(seat.id()))
                                            .toList();
                                }

                                @Override
                                public List<TicketSeat> findAllByEventId(
                                        final TicketEventId ticketEventId) {
                                    return fixture.seats.stream()
                                            .filter(
                                                    seat ->
                                                            seat.ticketEventId()
                                                                    .equals(ticketEventId))
                                            .toList();
                                }
                            },
                            new TicketSeatClaimPort() {
                                @Override
                                public boolean claimAvailableSeats(final TicketPurchase purchase) {
                                    final var claimed = new ArrayList<TicketSeat>();
                                    for (final var seat : fixture.seats) {
                                        if (purchase.seatIds().contains(seat.id())
                                                && seat.isAvailableFor(purchase.ticketEventId())) {
                                            claimed.add(seat.purchase(purchase.id(), NOW));
                                        }
                                    }
                                    if (claimed.size() != purchase.seatIds().size()) {
                                        return false;
                                    }
                                    fixture.savedPurchase = purchase;
                                    fixture.seats.clear();
                                    fixture.seats.addAll(claimed);
                                    return true;
                                }
                            },
                            new TicketPurchaseQueryPort() {
                                @Override
                                public Optional<TicketPurchase> findById(
                                        final TicketPurchaseId ticketPurchaseId) {
                                    return Optional.ofNullable(fixture.savedPurchase)
                                            .filter(
                                                    purchase ->
                                                            purchase.id().equals(ticketPurchaseId));
                                }

                                @Override
                                public Optional<TicketPurchase>
                                        findCustomerPurchaseForSeatSelection(
                                                final TicketEventId ticketEventId,
                                                final AccountId customerAccountId,
                                                final List<TicketSeatId> seatIds) {
                                    return Optional.ofNullable(fixture.savedPurchase)
                                            .filter(
                                                    purchase ->
                                                            purchase.ownsSameSelection(
                                                                    customerAccountId, seatIds));
                                }
                            },
                            new PurchaseConfirmationIdempotencyPort() {
                                @Override
                                public Optional<PurchaseConfirmationIdempotency> findForCustomerKey(
                                        final AccountId customerAccountId,
                                        final PurchaseConfirmationIdempotencyKey idempotencyKey) {
                                    return Optional.ofNullable(fixture.savedIdempotency)
                                            .filter(
                                                    idempotency ->
                                                            idempotency
                                                                            .customerAccountId()
                                                                            .equals(
                                                                                    customerAccountId)
                                                                    && idempotency
                                                                            .idempotencyKey()
                                                                            .equals(
                                                                                    idempotencyKey));
                                }

                                @Override
                                public PurchaseConfirmationIdempotency createPendingOrFindExisting(
                                        final PurchaseConfirmationIdempotency idempotency) {
                                    return save(idempotency);
                                }

                                @Override
                                public PurchaseConfirmationIdempotency save(
                                        final PurchaseConfirmationIdempotency idempotency) {
                                    fixture.savedIdempotency = idempotency;
                                    return idempotency;
                                }
                            },
                            clock);
            return fixture;
        }

        private static TicketEvent event() {
            return TicketingTestFixtures.event(
                    "Concert", Instant.parse("2026-06-03T01:00:00Z"), NOW);
        }
    }
}
