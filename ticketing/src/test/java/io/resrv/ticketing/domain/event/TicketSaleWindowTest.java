package io.resrv.ticketing.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class TicketSaleWindowTest {

    private static final Timezone SEOUL = Timezone.of("Asia/Seoul");

    @Test
    void preservesExactBoundaryInstantsAndTimezone() {
        final var start = Instant.parse("2026-06-03T00:00:00Z");
        final var end = Instant.parse("2026-06-03T00:00:01Z");

        final var window = new TicketSaleWindow(start, end, SEOUL);

        assertThat(window.startAt()).isEqualTo(start);
        assertThat(window.endAt()).isEqualTo(end);
        assertThat(window.timezone()).isEqualTo(SEOUL);
    }

    @Test
    void rejectsMissingEnd() {
        assertThatThrownBy(
                        () ->
                                new TicketSaleWindow(
                                        Instant.parse("2026-06-03T00:00:00Z"), null, SEOUL))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("end time");
    }

    @Test
    void rejectsInvertedRange() {
        assertThatThrownBy(
                        () ->
                                new TicketSaleWindow(
                                        Instant.parse("2026-06-03T00:00:01Z"),
                                        Instant.parse("2026-06-03T00:00:00Z"),
                                        SEOUL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsWindowOutsideEventOccurrence() {
        final var event =
                new TicketEventProfile(
                        "Concert",
                        Instant.parse("2026-06-10T00:00:00Z"),
                        Instant.parse("2026-06-10T02:00:00Z"),
                        SEOUL);
        final var sale =
                new TicketSaleWindow(
                        Instant.parse("2026-06-01T00:00:00Z"),
                        Instant.parse("2026-06-20T00:00:00Z"),
                        SEOUL);

        assertThat(event.eventStartAt()).isAfter(sale.startAt());
        assertThat(sale.endAt()).isAfter(event.eventEndAt());
    }
}
