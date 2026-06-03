package io.resrv.ticketing.support;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventProfile;
import io.resrv.ticketing.domain.event.TicketSaleWindow;
import java.time.Instant;

public final class TicketingTestFixtures {

    public static final Timezone SEOUL = Timezone.of("Asia/Seoul");

    private static final Instant DEFAULT_EVENT_START_AT = Instant.parse("2026-06-04T00:00:00Z");
    private static final Instant DEFAULT_EVENT_END_AT = Instant.parse("2026-06-04T02:00:00Z");
    private static final Instant DEFAULT_SALE_START_AT = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant DEFAULT_SALE_END_AT = Instant.parse("2026-06-03T00:00:00Z");

    private TicketingTestFixtures() {}

    public static TicketEvent event(final Instant createdAt) {
        return event("Concert", createdAt);
    }

    public static TicketEvent event(final String title, final Instant createdAt) {
        return event(title, DEFAULT_SALE_END_AT, createdAt);
    }

    public static TicketEvent event(
            final String title, final Instant saleEndAt, final Instant createdAt) {
        return TicketEvent.create(
                BusinessId.create(), eventProfile(title), saleWindow(saleEndAt), createdAt);
    }

    public static TicketEventProfile eventProfile(final String title) {
        return new TicketEventProfile(title, DEFAULT_EVENT_START_AT, DEFAULT_EVENT_END_AT, SEOUL);
    }

    public static TicketSaleWindow saleWindow() {
        return saleWindow(DEFAULT_SALE_END_AT);
    }

    public static TicketSaleWindow saleWindow(final Instant endAt) {
        return new TicketSaleWindow(DEFAULT_SALE_START_AT, endAt, SEOUL);
    }
}
