package io.resrv.ticketing.application.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.ticketing.application.event.out.TicketEventQueryPort;
import io.resrv.ticketing.application.inventory.in.CreateTicketInventoryCommand;
import io.resrv.ticketing.application.inventory.in.GetTicketInventoryQuery;
import io.resrv.ticketing.application.inventory.out.TicketInventoryCommandPort;
import io.resrv.ticketing.application.inventory.out.TicketInventoryQueryPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.inventory.TicketInventory;
import io.resrv.ticketing.domain.inventory.TicketInventoryId;
import io.resrv.ticketing.support.TicketingTestFixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class TicketInventorySetupServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    private final InMemoryEventPort eventPort = new InMemoryEventPort();
    private final InMemoryInventoryPort inventoryPort = new InMemoryInventoryPort();
    private final TicketInventorySetupService service =
            new TicketInventorySetupService(
                    eventPort, inventoryPort, inventoryPort, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsInventoryWithDerivedAvailableCounts() {
        final var event = activeEvent();
        eventPort.save(event);

        final var result =
                service.create(
                        new CreateTicketInventoryCommand(
                                event.id(),
                                List.of(
                                        new CreateTicketInventoryCommand.TierCommand(
                                                "General", 100, 10, 20, 5))));

        assertThat(result.ticketEventId()).isEqualTo(event.id());
        assertThat(result.tiers().getFirst().available()).isEqualTo(65);
        assertThat(service.find(new GetTicketInventoryQuery(result.id()))).hasValue(result);
    }

    @Test
    void rejectsInventoryForInactiveEvents() {
        final var event = activeEvent().deactivate(NOW.plusSeconds(60));
        eventPort.save(event);

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateTicketInventoryCommand(
                                                event.id(),
                                                List.of(
                                                        new CreateTicketInventoryCommand
                                                                .TierCommand(
                                                                "General", 10, 0, 0, 0)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inactive ticket events");
    }

    private static TicketEvent activeEvent() {
        return TicketingTestFixtures.event(NOW);
    }

    private static final class InMemoryEventPort implements TicketEventQueryPort {

        private final Map<TicketEventId, TicketEvent> events = new HashMap<>();

        void save(final TicketEvent event) {
            events.put(event.id(), event);
        }

        @Override
        public Optional<TicketEvent> findById(final TicketEventId ticketEventId) {
            return Optional.ofNullable(events.get(ticketEventId));
        }
    }

    private static final class InMemoryInventoryPort
            implements TicketInventoryCommandPort, TicketInventoryQueryPort {

        private final Map<TicketInventoryId, TicketInventory> inventories = new HashMap<>();

        @Override
        public void save(final TicketInventory inventory) {
            inventories.put(inventory.id(), inventory);
        }

        @Override
        public Optional<TicketInventory> findById(final TicketInventoryId ticketInventoryId) {
            return Optional.ofNullable(inventories.get(ticketInventoryId));
        }
    }
}
