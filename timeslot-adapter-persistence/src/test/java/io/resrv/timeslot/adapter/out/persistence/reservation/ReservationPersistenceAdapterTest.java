package io.resrv.timeslot.adapter.out.persistence.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.reservation.out.ReservationCommandPort;
import io.resrv.timeslot.application.reservation.out.ReservationQueryPort;
import io.resrv.timeslot.domain.reservation.Reservation;
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
@ComponentScan("io.resrv.timeslot.adapter.out.persistence")
class ReservationPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");
    private static final Instant START_AT = Instant.parse("2026-05-25T01:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-05-25T01:30:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private ReservationCommandPort commandPort;

    @Autowired private ReservationQueryPort queryPort;

    @Test
    void activeBlockerQueryExcludesExpiredHolds() {
        final var businessId = BusinessId.create();
        final var resourceId = ResourceId.create();
        final var expiredHold =
                Reservation.hold(
                        businessId,
                        resourceId,
                        AccountId.create(),
                        START_AT,
                        END_AT,
                        NOW.minusSeconds(60),
                        NOW.minusSeconds(120));
        final var activeHold =
                Reservation.hold(
                        businessId,
                        resourceId,
                        AccountId.create(),
                        START_AT,
                        END_AT,
                        NOW.plusSeconds(60),
                        NOW);
        commandPort.save(expiredHold);
        commandPort.save(activeHold);

        final var blockers =
                queryPort.findActiveBlockers(businessId, resourceId, START_AT, END_AT, NOW);

        assertEquals(1, blockers.size());
        assertEquals(activeHold.id(), blockers.getFirst().id());
    }
}
