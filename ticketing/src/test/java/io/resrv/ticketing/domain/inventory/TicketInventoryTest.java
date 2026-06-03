package io.resrv.ticketing.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.support.TicketingTestFixtures;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TicketInventoryTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Test
    void calculatesAvailableUnitsFromCounters() {
        final var tier = TicketInventoryTier.create("General", 100, 10, 20, 5);

        assertThat(tier.available()).isEqualTo(65);
    }

    @Test
    void allowsExplicitZeroCapacity() {
        final var tier = TicketInventoryTier.create("Closed", 0, 0, 0, 0);

        assertThat(tier.available()).isZero();
    }

    @Test
    void rejectsConsumedUnitsAboveTotal() {
        assertThatThrownBy(() -> TicketInventoryTier.create("Bad", 10, 5, 5, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed total");
    }

    @Test
    void rejectsNegativeCounters() {
        assertThatThrownBy(() -> TicketInventoryTier.create("Bad", 10, -1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void rejectsInventoryForInactiveEvent() {
        final var inactive = activeEvent().deactivate(NOW.plusSeconds(60));

        assertThatThrownBy(
                        () ->
                                TicketInventory.create(
                                        inactive,
                                        List.of(TicketInventoryTier.create("General", 10, 0, 0, 0)),
                                        NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inactive ticket events");
    }

    private static TicketEvent activeEvent() {
        return TicketingTestFixtures.event(NOW);
    }
}
