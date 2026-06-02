package io.resrv.ticketing.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class TicketEventLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");
    private static final Timezone SEOUL = Timezone.of("Asia/Seoul");

    @Test
    void activationAndDeactivationControlFutureClaimEligibility() {
        final var event =
                TicketEvent.create(BusinessId.create(), profile(), saleWindow(), NOW)
                        .deactivate(NOW.plusSeconds(60));

        assertThat(event.allowsFutureClaims()).isFalse();

        final var reactivated = event.activate(NOW.plusSeconds(120));

        assertThat(reactivated.allowsFutureClaims()).isTrue();
        assertThat(reactivated.id()).isEqualTo(event.id());
    }

    private static TicketEventProfile profile() {
        return new TicketEventProfile(
                "Concert",
                Instant.parse("2026-06-04T00:00:00Z"),
                Instant.parse("2026-06-04T02:00:00Z"),
                SEOUL);
    }

    private static TicketSaleWindow saleWindow() {
        return new TicketSaleWindow(
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-03T00:00:00Z"),
                SEOUL);
    }
}
