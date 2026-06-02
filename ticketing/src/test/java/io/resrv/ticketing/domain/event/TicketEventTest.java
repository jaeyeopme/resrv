package io.resrv.ticketing.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class TicketEventTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");
    private static final Timezone SEOUL = Timezone.of("Asia/Seoul");

    @Test
    void createsEventWithIdentityProfileSaleWindowAndActiveStatus() {
        final var event =
                TicketEvent.create(
                        BusinessId.create(), profile("Launch Concert"), saleWindow(), NOW);

        assertThat(event.id()).isNotNull();
        assertThat(event.profile().title()).isEqualTo("Launch Concert");
        assertThat(event.saleWindow().startAt()).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(event.status()).isEqualTo(TicketEventStatus.ACTIVE);
        assertThat(event.allowsFutureClaims()).isTrue();
    }

    @Test
    void acceptsDuplicateTitlesBecauseTitleIsNotIdentity() {
        final var businessId = BusinessId.create();
        final var first = TicketEvent.create(businessId, profile("Same Name"), saleWindow(), NOW);
        final var second = TicketEvent.create(businessId, profile("Same Name"), saleWindow(), NOW);

        assertThat(first.profile().title()).isEqualTo(second.profile().title());
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void rejectsInvertedEventOccurrence() {
        assertThatThrownBy(
                        () ->
                                new TicketEventProfile(
                                        "Bad",
                                        Instant.parse("2026-06-04T01:00:00Z"),
                                        Instant.parse("2026-06-04T00:00:00Z"),
                                        SEOUL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event start time");
    }

    @Test
    void rejectsInvertedSaleWindow() {
        assertThatThrownBy(
                        () ->
                                new TicketSaleWindow(
                                        Instant.parse("2026-06-02T00:00:00Z"),
                                        Instant.parse("2026-06-01T00:00:00Z"),
                                        SEOUL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sale window start time");
    }

    @Test
    void inactiveStatusBlocksFutureClaims() {
        final var event =
                TicketEvent.create(BusinessId.create(), profile("Concert"), saleWindow(), NOW)
                        .deactivate(NOW.plusSeconds(60));

        assertThat(event.status()).isEqualTo(TicketEventStatus.INACTIVE);
        assertThat(event.allowsFutureClaims()).isFalse();
    }

    private static TicketEventProfile profile(final String title) {
        return new TicketEventProfile(
                title,
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
