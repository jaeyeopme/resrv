package io.resrv.ticketing.adapter.out.persistence.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.ticketing.application.event.out.TicketEventCommandPort;
import io.resrv.ticketing.application.event.out.TicketEventQueryPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.support.TicketingTestFixtures;
import java.time.Instant;
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
class TicketEventPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private TicketEventCommandPort commandPort;

    @Autowired private TicketEventQueryPort queryPort;

    @Test
    void savesAndLoadsTicketEventWithImmutableIdentity() {
        final var event = event("Concert");

        commandPort.save(event);

        final var found = queryPort.findById(event.id()).orElseThrow();
        assertThat(found.id()).isEqualTo(event.id());
        assertThat(found.businessId()).isEqualTo(event.businessId());
        assertThat(found.profile().title()).isEqualTo("Concert");
        assertThat(found.profile().timezone()).isEqualTo(TicketingTestFixtures.SEOUL);
        assertThat(found.saleWindow().startAt()).isEqualTo(event.saleWindow().startAt());
        assertThat(found.createdAt()).isEqualTo(NOW);
    }

    @Test
    void persistsDuplicateTitlesBecauseTitleIsNotIdentity() {
        final var first = event("Same Name");
        final var second = event("Same Name");

        commandPort.save(first);
        commandPort.save(second);

        assertThat(queryPort.findById(first.id())).isPresent();
        assertThat(queryPort.findById(second.id())).isPresent();
    }

    private static TicketEvent event(final String title) {
        return TicketingTestFixtures.event(title, NOW);
    }
}
