package io.resrv.ticketing.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventProfile;
import io.resrv.ticketing.domain.event.TicketSaleWindow;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TicketInventoryTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");
    private static final Timezone SEOUL = Timezone.of("Asia/Seoul");

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
        return TicketEvent.create(
                BusinessId.create(),
                new TicketEventProfile(
                        "Concert",
                        Instant.parse("2026-06-04T00:00:00Z"),
                        Instant.parse("2026-06-04T02:00:00Z"),
                        SEOUL),
                new TicketSaleWindow(
                        Instant.parse("2026-06-01T00:00:00Z"),
                        Instant.parse("2026-06-03T00:00:00Z"),
                        SEOUL),
                NOW);
    }
}
