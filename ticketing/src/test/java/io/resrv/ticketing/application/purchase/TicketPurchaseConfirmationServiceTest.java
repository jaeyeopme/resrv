package io.resrv.ticketing.application.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.ticketing.application.purchase.in.ConfirmTicketPurchaseCommand;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseQueryPort;
import io.resrv.ticketing.application.seat.out.TicketSeatCommandPort;
import io.resrv.ticketing.application.seat.out.TicketSeatQueryPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.event.TicketEventProfile;
import io.resrv.ticketing.domain.event.TicketSaleWindow;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
import io.resrv.ticketing.domain.purchase.TicketPurchaseId;
import io.resrv.ticketing.domain.seat.TicketSeat;
import io.resrv.ticketing.domain.seat.TicketSeatId;
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
                                fixture.event.id(), fixture.customerId, seatIds));

        assertThat(result.seatIds())
                .containsExactlyElementsOf(seatIds.stream().map(TicketSeatId::value).toList());
        assertThat(fixture.savedPurchase).isNotNull();
        assertThat(fixture.savedSeats)
                .allSatisfy(seat -> assertThat(seat.purchaseId()).isNotNull());
    }

    @Test
    void rejectsUnavailableSeatsWithoutSavingAttempt() {
        final var fixture = Fixture.create();
        final var soldSeat =
                fixture.seats.getFirst().purchase(TicketPurchaseId.create(), NOW.minusSeconds(60));
        fixture.seats.set(0, soldSeat);

        assertThatThrownBy(
                        () ->
                                fixture.service.confirm(
                                        new ConfirmTicketPurchaseCommand(
                                                fixture.event.id(),
                                                fixture.customerId,
                                                fixture.seats.stream()
                                                        .map(TicketSeat::id)
                                                        .toList())))
                .isInstanceOf(TicketPurchaseValidationException.class)
                .hasMessage("Selected seats are unavailable");
        assertThat(fixture.savedPurchase).isNull();
    }

    @Test
    void rejectsPurchaseBeforeSaleWindowOpens() {
        final var fixture =
                Fixture.create(Clock.fixed(Instant.parse("2026-05-31T23:59:59Z"), ZoneOffset.UTC));

        assertThatThrownBy(
                        () ->
                                fixture.service.confirm(
                                        new ConfirmTicketPurchaseCommand(
                                                fixture.event.id(),
                                                fixture.customerId,
                                                fixture.seats.stream()
                                                        .map(TicketSeat::id)
                                                        .toList())))
                .isInstanceOf(TicketPurchaseValidationException.class)
                .hasMessage("Ticket event is not available for sale");
        assertThat(fixture.savedPurchase).isNull();
    }

    @Test
    void rejectsPurchaseAfterSaleWindowCloses() {
        final var fixture =
                Fixture.create(Clock.fixed(Instant.parse("2026-06-03T01:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(
                        () ->
                                fixture.service.confirm(
                                        new ConfirmTicketPurchaseCommand(
                                                fixture.event.id(),
                                                fixture.customerId,
                                                fixture.seats.stream()
                                                        .map(TicketSeat::id)
                                                        .toList())))
                .isInstanceOf(TicketPurchaseValidationException.class)
                .hasMessage("Ticket event is not available for sale");
        assertThat(fixture.savedPurchase).isNull();
    }

    private static final class Fixture {

        final TicketEvent event = event();
        final AccountId customerId = AccountId.create();
        final List<TicketSeat> seats =
                new ArrayList<>(
                        List.of(
                                TicketSeat.createAvailable(event.id(), "A-1"),
                                TicketSeat.createAvailable(event.id(), "A-2")));
        TicketPurchase savedPurchase;
        List<TicketSeat> savedSeats = List.of();
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
                            new TicketSeatCommandPort() {
                                @Override
                                public void save(final TicketSeat seat) {
                                    fixture.savedSeats = List.of(seat);
                                }

                                @Override
                                public void saveAll(final List<TicketSeat> seats) {
                                    fixture.savedSeats = List.copyOf(seats);
                                }
                            },
                            purchase -> fixture.savedPurchase = purchase,
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
                            clock);
            return fixture;
        }

        private static TicketEvent event() {
            final var timezone = Timezone.of("Asia/Seoul");
            return TicketEvent.create(
                    BusinessId.create(),
                    new TicketEventProfile(
                            "Concert",
                            Instant.parse("2026-06-04T00:00:00Z"),
                            Instant.parse("2026-06-04T02:00:00Z"),
                            timezone),
                    new TicketSaleWindow(
                            Instant.parse("2026-06-01T00:00:00Z"),
                            Instant.parse("2026-06-03T01:00:00Z"),
                            timezone),
                    NOW);
        }
    }
}
