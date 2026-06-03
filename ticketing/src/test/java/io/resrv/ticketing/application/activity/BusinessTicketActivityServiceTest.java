package io.resrv.ticketing.application.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.ticketing.application.activity.in.BusinessTicketActivityQuery;
import io.resrv.ticketing.application.activity.out.TicketPurchaseActivityQueryPort;
import io.resrv.ticketing.application.platform.out.TicketingBusinessAccessPort;
import io.resrv.ticketing.application.purchase.TicketPurchaseAccessDeniedException;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.support.TicketingTestFixtures;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class BusinessTicketActivityServiceTest {

    @Test
    void authorizedBusinessActorCanReviewPurchases() {
        final var event = event();
        final var service =
                new BusinessTicketActivityService(
                        eventId -> Optional.of(event),
                        accessPort(true),
                        new FakeActivityQueryPort(
                                List.of(
                                        new TicketPurchaseActivityQueryPort.BusinessPurchaseView(
                                                UUID.randomUUID(),
                                                event.id().value(),
                                                "Concert",
                                                AccountId.create().value(),
                                                List.of(),
                                                Instant.parse("2026-06-03T00:00:00Z")))));

        final var result =
                service.list(new BusinessTicketActivityQuery(AccountId.create(), event.id()));

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void unauthorizedBusinessActorGetsNonEnumeratingNotFound() {
        final var event = event();
        final var service =
                new BusinessTicketActivityService(
                        eventId -> Optional.of(event),
                        accessPort(false),
                        new FakeActivityQueryPort(List.of()));

        assertThatThrownBy(
                        () ->
                                service.list(
                                        new BusinessTicketActivityQuery(
                                                AccountId.create(), event.id())))
                .isInstanceOf(TicketPurchaseAccessDeniedException.class)
                .hasMessage("Ticket event not found");
    }

    private record FakeActivityQueryPort(
            List<TicketPurchaseActivityQueryPort.BusinessPurchaseView> purchases)
            implements TicketPurchaseActivityQueryPort {

        @Override
        public List<CustomerPurchaseView> findCustomerPurchases(final AccountId customerAccountId) {
            return List.of();
        }

        @Override
        public List<BusinessPurchaseView> findBusinessEventPurchases(
                final BusinessId businessId, final TicketEventId ticketEventId) {
            return purchases;
        }
    }

    private static TicketingBusinessAccessPort accessPort(final boolean hasAccess) {
        return new TicketingBusinessAccessPort() {
            @Override
            public Optional<BusinessView> findActiveBusiness(final BusinessId businessId) {
                return Optional.empty();
            }

            @Override
            public boolean hasBusinessAccess(
                    final AccountId accountId, final BusinessId businessId) {
                return hasAccess;
            }
        };
    }

    private static TicketEvent event() {
        return TicketingTestFixtures.event(
                "Concert",
                Instant.parse("2026-06-03T01:00:00Z"),
                Instant.parse("2026-06-03T00:00:00Z"));
    }
}
