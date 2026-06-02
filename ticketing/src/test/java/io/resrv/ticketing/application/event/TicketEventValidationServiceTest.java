package io.resrv.ticketing.application.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.Timezone;
import io.resrv.ticketing.domain.event.TicketEventProfile;
import io.resrv.ticketing.domain.event.TicketSaleWindow;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class TicketEventValidationServiceTest {

    private static final Timezone SEOUL = Timezone.of("Asia/Seoul");
    private final TicketEventValidationService service = new TicketEventValidationService();

    @Test
    void acceptsValidProfileAndSaleWindow() {
        assertThatCode(() -> service.validate(profile(), saleWindow())).doesNotThrowAnyException();
    }

    @Test
    void invalidSaleWindowIsRejectedBeforeValidationServiceRuns() {
        assertThatThrownBy(
                        () ->
                                new TicketSaleWindow(
                                        Instant.parse("2026-06-03T00:00:00Z"),
                                        Instant.parse("2026-06-02T00:00:00Z"),
                                        SEOUL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sale window start time");
    }

    @Test
    void invalidEventOccurrenceIsRejectedBeforeValidationServiceRuns() {
        assertThatThrownBy(
                        () ->
                                new TicketEventProfile(
                                        "Bad",
                                        Instant.parse("2026-06-04T02:00:00Z"),
                                        Instant.parse("2026-06-04T00:00:00Z"),
                                        SEOUL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event start time");
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
