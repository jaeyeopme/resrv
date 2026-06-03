package io.resrv.ticketing.application.activity;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.ticketing.application.activity.in.CustomerTicketHistoryQuery;
import io.resrv.ticketing.application.activity.out.TicketPurchaseActivityQueryPort;
import io.resrv.ticketing.domain.event.TicketEventId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CustomerTicketHistoryServiceTest {

    @Test
    void returnsOnlyViewsProvidedForAuthenticatedCustomer() {
        final var customerId = AccountId.create();
        final var purchase =
                new TicketPurchaseActivityQueryPort.CustomerPurchaseView(
                        UUID.randomUUID(),
                        TicketEventId.create().value(),
                        "Concert",
                        Instant.parse("2026-06-04T00:00:00Z"),
                        List.of(
                                new TicketPurchaseActivityQueryPort.SeatView(
                                        UUID.randomUUID(), "A-1")),
                        Instant.parse("2026-06-03T00:00:00Z"));
        final var service =
                new CustomerTicketHistoryService(
                        new FakeActivityQueryPort(List.of(purchase), List.of()));

        final var result = service.list(new CustomerTicketHistoryQuery(customerId));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().eventTitle()).isEqualTo("Concert");
    }

    @Test
    void emptyHistoryIsReturnedAsEmptyItems() {
        final var service =
                new CustomerTicketHistoryService(new FakeActivityQueryPort(List.of(), List.of()));

        final var result = service.list(new CustomerTicketHistoryQuery(AccountId.create()));

        assertThat(result.items()).isEmpty();
    }

    private record FakeActivityQueryPort(
            List<TicketPurchaseActivityQueryPort.CustomerPurchaseView> customerPurchases,
            List<TicketPurchaseActivityQueryPort.BusinessPurchaseView> businessPurchases)
            implements TicketPurchaseActivityQueryPort {

        @Override
        public List<CustomerPurchaseView> findCustomerPurchases(final AccountId customerAccountId) {
            return customerPurchases;
        }

        @Override
        public List<BusinessPurchaseView> findBusinessEventPurchases(
                final BusinessId businessId, final TicketEventId ticketEventId) {
            return businessPurchases;
        }
    }
}
