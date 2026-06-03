package io.resrv.ticketing.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.ticketing.support.TicketingTestFixtures;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class TicketEventLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Test
    void activationAndDeactivationControlFutureClaimEligibility() {
        final var event =
                TicketEvent.create(
                                BusinessId.create(),
                                TicketingTestFixtures.eventProfile("Concert"),
                                TicketingTestFixtures.saleWindow(),
                                NOW)
                        .deactivate(NOW.plusSeconds(60));

        assertThat(event.allowsFutureClaims()).isFalse();

        final var reactivated = event.activate(NOW.plusSeconds(120));

        assertThat(reactivated.allowsFutureClaims()).isTrue();
        assertThat(reactivated.id()).isEqualTo(event.id());
    }
}
