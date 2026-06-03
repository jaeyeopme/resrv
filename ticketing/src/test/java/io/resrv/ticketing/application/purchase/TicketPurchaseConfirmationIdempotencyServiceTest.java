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

final class TicketPurchaseConfirmationIdempotencyServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Test
    void replaysSuccessfulPurchaseForSameKeyAndRequest() {
        final var fixture = Fixture.create();
        final var command = fixture.command("purchase-key", fixture.firstSeatId());

        final var first = fixture.service.confirm(command);
        final var replay = fixture.service.confirm(command);

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(fixture.claimAttempts).isEqualTo(1);
    }

    @Test
    void replaysUnavailableOutcomeForSameKeyAndRequest() {
        final var fixture = Fixture.create();
        fixture.claimSucceeds = false;
        final var command = fixture.command("purchase-key", fixture.firstSeatId());

        final var first = fixture.service.confirm(command);
        final var replay = fixture.service.confirm(command);

        assertThat(first.outcome()).isEqualTo(replay.outcome());
        assertThat(replay.purchased()).isFalse();
        assertThat(fixture.claimAttempts).isEqualTo(1);
    }

    @Test
    void rejectsChangedPayloadAndExpiredKey() {
        final var fixture = Fixture.create();
        fixture.service.confirm(fixture.command("purchase-key", fixture.firstSeatId()));

        assertThatThrownBy(
                        () ->
                                fixture.service.confirm(
                                        fixture.command("purchase-key", fixture.secondSeatId())))
                .isInstanceOf(TicketPurchaseIdempotencyException.class)
                .hasMessage("Idempotency key was already used with different purchase details");

        final var expiredFixture =
                Fixture.create(
                        Clock.fixed(
                                NOW.plus(PurchaseConfirmationIdempotency.REPLAY_WINDOW),
                                ZoneOffset.UTC));
        expiredFixture.idempotencies.add(
                PurchaseConfirmationIdempotency.pending(
                        PurchaseConfirmationIdempotencyKey.of("expired-key"),
                        expiredFixture.customerId,
                        expiredFixture.event.id(),
                        List.of(expiredFixture.firstSeatId()),
                        NOW));

        assertThatThrownBy(
                        () ->
                                expiredFixture.service.confirm(
                                        expiredFixture.command(
                                                "expired-key", expiredFixture.firstSeatId())))
                .isInstanceOf(TicketPurchaseIdempotencyException.class)
                .hasMessage("Idempotency key replay window has expired");
    }

    private static final class Fixture {

        final TicketEvent event =
                TicketingTestFixtures.event("Concert", Instant.parse("2026-06-03T01:00:00Z"), NOW);
        final AccountId customerId = AccountId.create();
        final List<TicketSeat> seats =
                List.of(
                        TicketSeat.createAvailable(event.id(), "A-1"),
                        TicketSeat.createAvailable(event.id(), "A-2"));
        final List<PurchaseConfirmationIdempotency> idempotencies = new ArrayList<>();
        TicketPurchase purchase;
        int claimAttempts;
        boolean claimSucceeds = true;
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
                                    return fixture.seats;
                                }
                            },
                            new TicketSeatClaimPort() {
                                @Override
                                public boolean claimAvailableSeats(final TicketPurchase purchase) {
                                    fixture.claimAttempts++;
                                    if (fixture.claimSucceeds) {
                                        fixture.purchase = purchase;
                                    }
                                    return fixture.claimSucceeds;
                                }
                            },
                            new TicketPurchaseQueryPort() {
                                @Override
                                public Optional<TicketPurchase> findById(
                                        final TicketPurchaseId ticketPurchaseId) {
                                    return Optional.ofNullable(fixture.purchase)
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
                                    return Optional.ofNullable(fixture.purchase)
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
                                    return fixture.idempotencies.stream()
                                            .filter(
                                                    idempotency ->
                                                            idempotency
                                                                            .customerAccountId()
                                                                            .equals(
                                                                                    customerAccountId)
                                                                    && idempotency
                                                                            .idempotencyKey()
                                                                            .equals(idempotencyKey))
                                            .findFirst();
                                }

                                @Override
                                public PurchaseConfirmationIdempotency createPendingOrFindExisting(
                                        final PurchaseConfirmationIdempotency idempotency) {
                                    return save(idempotency);
                                }

                                @Override
                                public PurchaseConfirmationIdempotency save(
                                        final PurchaseConfirmationIdempotency idempotency) {
                                    fixture.idempotencies.removeIf(
                                            existing ->
                                                    existing.customerAccountId()
                                                                    .equals(
                                                                            idempotency
                                                                                    .customerAccountId())
                                                            && existing.idempotencyKey()
                                                                    .equals(
                                                                            idempotency
                                                                                    .idempotencyKey()));
                                    fixture.idempotencies.add(idempotency);
                                    return idempotency;
                                }
                            },
                            clock);
            return fixture;
        }

        ConfirmTicketPurchaseCommand command(final String key, final TicketSeatId seatId) {
            return new ConfirmTicketPurchaseCommand(
                    event.id(),
                    customerId,
                    List.of(seatId),
                    PurchaseConfirmationIdempotencyKey.of(key));
        }

        TicketSeatId firstSeatId() {
            return seats.getFirst().id();
        }

        TicketSeatId secondSeatId() {
            return seats.get(1).id();
        }
    }
}
