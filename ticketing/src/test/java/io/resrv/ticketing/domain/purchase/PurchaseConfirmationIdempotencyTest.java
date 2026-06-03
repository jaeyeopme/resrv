package io.resrv.ticketing.domain.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PurchaseConfirmationIdempotencyTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Test
    void fingerprintsSeatSelectionDeterministicallyAndTracksExpiry() {
        final var eventId = TicketEventId.create();
        final var firstSeatId = TicketSeatId.create();
        final var secondSeatId = TicketSeatId.create();
        final var idempotency =
                PurchaseConfirmationIdempotency.pending(
                        PurchaseConfirmationIdempotencyKey.of("purchase-key"),
                        AccountId.create(),
                        eventId,
                        List.of(firstSeatId, secondSeatId),
                        NOW);

        assertThat(idempotency.matches(eventId, List.of(secondSeatId, firstSeatId))).isTrue();
        assertThat(idempotency.expiredAt(NOW.plusSeconds(86_399))).isFalse();
        assertThat(idempotency.expiredAt(NOW.plusSeconds(86_400))).isTrue();
        assertThat(idempotency.cleanupEligibleAt())
                .isEqualTo(
                        NOW.plus(PurchaseConfirmationIdempotency.REPLAY_WINDOW)
                                .plus(PurchaseConfirmationIdempotency.EXPIRED_RETENTION));
    }

    @Test
    void rejectsBlankAndOversizedIdempotencyKeys() {
        assertThatThrownBy(() -> PurchaseConfirmationIdempotencyKey.of(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Purchase confirmation idempotency key is required");
        assertThatThrownBy(() -> PurchaseConfirmationIdempotencyKey.of("a".repeat(121)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Purchase confirmation idempotency key must be at most 120 characters");
    }
}
