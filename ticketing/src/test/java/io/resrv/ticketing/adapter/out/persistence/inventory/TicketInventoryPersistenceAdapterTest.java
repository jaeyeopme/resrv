package io.resrv.ticketing.adapter.out.persistence.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.ticketing.application.event.out.TicketEventCommandPort;
import io.resrv.ticketing.application.inventory.out.TicketInventoryCommandPort;
import io.resrv.ticketing.application.inventory.out.TicketInventoryQueryPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.inventory.TicketInventory;
import io.resrv.ticketing.domain.inventory.TicketInventoryTier;
import io.resrv.ticketing.support.TicketingTestFixtures;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan("io.resrv.ticketing.adapter.out.persistence")
class TicketInventoryPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private TicketEventCommandPort eventCommandPort;

    @Autowired private TicketInventoryCommandPort inventoryCommandPort;

    @Autowired private TicketInventoryQueryPort inventoryQueryPort;

    @Test
    void savesAndLoadsInventoryWithTierSnapshots() {
        final var event = event();
        eventCommandPort.save(event);
        final var inventory =
                TicketInventory.create(
                        event,
                        List.of(
                                TicketInventoryTier.create("General", 100, 10, 20, 5),
                                TicketInventoryTier.create("VIP", 10, 0, 2, 1)),
                        NOW);

        inventoryCommandPort.save(inventory);

        final var found = inventoryQueryPort.findById(inventory.id()).orElseThrow();
        assertThat(found.id()).isEqualTo(inventory.id());
        assertThat(found.ticketEventId()).isEqualTo(event.id());
        assertThat(found.tiers()).hasSize(2);
        assertThat(found.tiers().get(0).available()).isEqualTo(65);
        assertThat(found.tiers().get(1).available()).isEqualTo(7);
    }

    private static TicketEvent event() {
        return TicketingTestFixtures.event(NOW);
    }
}
