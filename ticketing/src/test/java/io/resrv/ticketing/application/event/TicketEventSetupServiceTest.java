package io.resrv.ticketing.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.ticketing.application.TicketingValidationException;
import io.resrv.ticketing.application.event.in.CreateTicketEventCommand;
import io.resrv.ticketing.application.event.in.GetTicketEventQuery;
import io.resrv.ticketing.application.event.out.TicketEventCommandPort;
import io.resrv.ticketing.application.event.out.TicketEventQueryPort;
import io.resrv.ticketing.application.platform.out.TicketingBusinessAccessPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class TicketEventSetupServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");
    private static final Timezone SEOUL = Timezone.of("Asia/Seoul");

    private final FakeBusinessAccessPort businessAccessPort = new FakeBusinessAccessPort();
    private final InMemoryTicketEventPort eventPort = new InMemoryTicketEventPort();
    private final TicketEventSetupService service =
            new TicketEventSetupService(
                    businessAccessPort,
                    eventPort,
                    eventPort,
                    new TicketEventValidationService(),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsEventWhenBusinessIsActive() {
        final var businessId = BusinessId.create();
        businessAccessPort.addBusiness(businessId);

        final var result = service.create(command(businessId, "Concert"));

        assertThat(result.id()).isNotNull();
        assertThat(result.businessId()).isEqualTo(businessId);
        assertThat(result.title()).isEqualTo("Concert");
        assertThat(service.find(new GetTicketEventQuery(result.id()))).hasValue(result);
    }

    @Test
    void acceptsDuplicateTitlesForSameBusiness() {
        final var businessId = BusinessId.create();
        businessAccessPort.addBusiness(businessId);

        final var first = service.create(command(businessId, "Concert"));
        final var second = service.create(command(businessId, "Concert"));

        assertThat(first.title()).isEqualTo(second.title());
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void rejectsEventWhenBusinessIsUnavailable() {
        final var businessId = BusinessId.create();

        assertThatThrownBy(() -> service.create(command(businessId, "Concert")))
                .isInstanceOf(TicketingValidationException.class)
                .hasMessageContaining("Business is not available");
    }

    private static CreateTicketEventCommand command(
            final BusinessId businessId, final String title) {
        return new CreateTicketEventCommand(
                businessId,
                title,
                Instant.parse("2026-06-04T00:00:00Z"),
                Instant.parse("2026-06-04T02:00:00Z"),
                SEOUL,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-03T00:00:00Z"),
                SEOUL);
    }

    private static final class FakeBusinessAccessPort implements TicketingBusinessAccessPort {

        private final Map<BusinessId, BusinessView> businesses = new HashMap<>();

        void addBusiness(final BusinessId businessId) {
            businesses.put(businessId, new BusinessView(businessId, "Business", SEOUL));
        }

        @Override
        public Optional<BusinessView> findActiveBusiness(final BusinessId businessId) {
            return Optional.ofNullable(businesses.get(businessId));
        }

        @Override
        public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
            return businesses.containsKey(businessId);
        }
    }

    private static final class InMemoryTicketEventPort
            implements TicketEventCommandPort, TicketEventQueryPort {

        private final Map<TicketEventId, TicketEvent> events = new HashMap<>();

        @Override
        public void save(final TicketEvent event) {
            events.put(event.id(), event);
        }

        @Override
        public Optional<TicketEvent> findById(final TicketEventId ticketEventId) {
            return Optional.ofNullable(events.get(ticketEventId));
        }
    }
}
