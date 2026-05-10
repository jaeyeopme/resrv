package io.resrv.adapter.out.persistence.availability;

import static io.resrv.adapter.out.persistence.PersistenceTestFixtures.insertResourceDirectly;
import static io.resrv.adapter.out.persistence.PersistenceTestFixtures.insertTenantDirectly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.domain.availability.DateAvailabilityOverride;
import io.resrv.domain.availability.WeeklyAvailability;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AvailabilityPersistenceAdapter.class)
class AvailabilityPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private AvailabilityPersistenceAdapter adapter;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void weeklyAvailability_saveFindAndDelete() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "availability-weekly");
        final var resourceId = insertResourceDirectly(jdbcTemplate, NOW, tenantId, "weekly-room");
        final var availability =
                WeeklyAvailability.create(
                        tenantId,
                        resourceId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        NOW);

        adapter.save(availability);

        final var found =
                adapter.findByTenantIdAndResourceIdAndDayOfWeek(
                                tenantId, resourceId, DayOfWeek.MONDAY)
                        .orElseThrow();
        assertEquals(availability.id(), found.id());
        assertEquals(LocalTime.of(9, 0), found.startTime());
        assertEquals(LocalTime.of(18, 0), found.endTime());
        assertFalse(
                adapter.findByTenantIdAndResourceIdAndDayOfWeek(
                                tenantId, resourceId, DayOfWeek.TUESDAY)
                        .isPresent());

        adapter.deleteByTenantIdAndResourceIdAndDayOfWeek(tenantId, resourceId, DayOfWeek.MONDAY);

        assertFalse(
                adapter.findByTenantIdAndResourceIdAndDayOfWeek(
                                tenantId, resourceId, DayOfWeek.MONDAY)
                        .isPresent());
    }

    @Test
    void dateAvailabilityOverride_openAndClosed_saveFindAndDelete() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "availability-date");
        final var resourceId = insertResourceDirectly(jdbcTemplate, NOW, tenantId, "date-room");
        final var openDate = LocalDate.of(2025, 1, 2);
        final var closedDate = LocalDate.of(2025, 1, 3);
        final var open =
                DateAvailabilityOverride.open(
                        tenantId,
                        resourceId,
                        openDate,
                        LocalTime.of(10, 0),
                        LocalTime.of(15, 0),
                        NOW);
        final var closed = DateAvailabilityOverride.closed(tenantId, resourceId, closedDate, NOW);

        adapter.save(open);
        adapter.save(closed);

        final var foundOpen =
                adapter.findByTenantIdAndResourceIdAndDate(tenantId, resourceId, openDate)
                        .orElseThrow();
        assertFalse(foundOpen.closed());
        assertEquals(LocalTime.of(10, 0), foundOpen.startTime());
        assertEquals(LocalTime.of(15, 0), foundOpen.endTime());

        final var foundClosed =
                adapter.findByTenantIdAndResourceIdAndDate(tenantId, resourceId, closedDate)
                        .orElseThrow();
        assertTrue(foundClosed.closed());
        assertNull(foundClosed.startTime());
        assertNull(foundClosed.endTime());

        adapter.deleteByTenantIdAndResourceIdAndDate(tenantId, resourceId, openDate);
        adapter.deleteByTenantIdAndResourceIdAndDate(tenantId, resourceId, closedDate);

        assertFalse(
                adapter.findByTenantIdAndResourceIdAndDate(tenantId, resourceId, openDate)
                        .isPresent());
        assertFalse(
                adapter.findByTenantIdAndResourceIdAndDate(tenantId, resourceId, closedDate)
                        .isPresent());
    }
}
