package io.resrv.adapter.out.persistence.reservation;

import static io.resrv.adapter.out.persistence.PersistenceTestFixtures.insertResourceDirectly;
import static io.resrv.adapter.out.persistence.PersistenceTestFixtures.insertTenantDirectly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.reservation.Reservation;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.reservation.SlotUnavailableException;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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
@Import(ReservationPersistenceAdapter.class)
class ReservationPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant START_AT = Instant.parse("2025-01-02T09:00:00Z");
    private static final Instant END_AT = Instant.parse("2025-01-02T10:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private ReservationPersistenceAdapter adapter;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void saveFindAndQueryReservations() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "reservation-save");
        final var resourceId = insertResourceDirectly(jdbcTemplate, NOW, tenantId, "save-room");
        final var customerId = insertCustomerDirectly(tenantId, "save@example.com");
        final var reservation = holdReservation(tenantId, resourceId, customerId, START_AT, END_AT);

        adapter.save(reservation);

        final var found = adapter.findByTenantIdAndId(tenantId, reservation.id()).orElseThrow();
        assertEquals(reservation.id(), found.id());
        assertEquals(ReservationStatus.HELD, found.status());

        final var byCustomer = adapter.findByTenantIdAndCustomerId(tenantId, customerId);
        assertEquals(1, byCustomer.size());
        assertEquals(reservation.id(), byCustomer.getFirst().id());

        final var byResourceWindow =
                adapter.findByTenantIdAndResourceIdBetween(
                        tenantId, resourceId, START_AT.minusSeconds(60), END_AT.plusSeconds(60));
        assertEquals(1, byResourceWindow.size());
        assertEquals(reservation.id(), byResourceWindow.getFirst().id());

        assertTrue(
                adapter.existsActiveOverlap(
                        tenantId, resourceId, START_AT.plusSeconds(60), END_AT.minusSeconds(60)));
        assertFalse(
                adapter.existsActiveOverlap(
                        tenantId, resourceId, END_AT, END_AT.plusSeconds(3600)));
    }

    @Test
    void overlappingActiveReservation_throwsSlotUnavailableException() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "reservation-overlap");
        final var resourceId = insertResourceDirectly(jdbcTemplate, NOW, tenantId, "overlap-room");
        final var customerId = insertCustomerDirectly(tenantId, "overlap@example.com");
        adapter.save(holdReservation(tenantId, resourceId, customerId, START_AT, END_AT));

        final var overlapping =
                holdReservation(
                        tenantId,
                        resourceId,
                        customerId,
                        START_AT.plusSeconds(300),
                        END_AT.plusSeconds(300));

        assertThrows(SlotUnavailableException.class, () -> adapter.save(overlapping));
    }

    @Test
    void expireHoldsDueAtOrBefore_marksHeldReservationsExpired() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "reservation-expire");
        final var resourceId = insertResourceDirectly(jdbcTemplate, NOW, tenantId, "expire-room");
        final var customerId = insertCustomerDirectly(tenantId, "expire@example.com");
        final var reservation =
                Reservation.hold(
                        tenantId,
                        resourceId,
                        customerId,
                        START_AT,
                        END_AT,
                        NOW.plusSeconds(60),
                        NOW);
        adapter.save(reservation);

        final var expiredCount = adapter.expireHoldsDueAtOrBefore(NOW.plusSeconds(120));

        assertEquals(1, expiredCount);
        final var found = adapter.findByTenantIdAndId(tenantId, reservation.id()).orElseThrow();
        assertEquals(ReservationStatus.EXPIRED, found.status());
        assertFalse(adapter.existsActiveOverlap(tenantId, resourceId, START_AT, END_AT));
    }

    @Test
    void cancelledReservationDoesNotBlockOverlap() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "reservation-cancelled");
        final var resourceId =
                insertResourceDirectly(jdbcTemplate, NOW, tenantId, "cancelled-room");
        final var customerId = insertCustomerDirectly(tenantId, "cancelled@example.com");
        final var cancelled =
                holdReservation(tenantId, resourceId, customerId, START_AT, END_AT)
                        .cancelByAdmin(NOW.plusSeconds(30));

        adapter.save(cancelled);

        assertFalse(adapter.existsActiveOverlap(tenantId, resourceId, START_AT, END_AT));
    }

    @Test
    void adminFilteredQueryCombinesTenantDateResourceCustomerAndStatus() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "reservation-filter");
        final var otherTenantId =
                insertTenantDirectly(jdbcTemplate, NOW, "reservation-filter-other");
        final var resourceId = insertResourceDirectly(jdbcTemplate, NOW, tenantId, "filter-room");
        final var otherResourceId =
                insertResourceDirectly(jdbcTemplate, NOW, tenantId, "filter-room-other");
        final var foreignResourceId =
                insertResourceDirectly(jdbcTemplate, NOW, otherTenantId, "filter-foreign-room");
        final var customerId = insertCustomerDirectly(tenantId, "filter@example.com");
        final var otherCustomerId = insertCustomerDirectly(tenantId, "filter-other@example.com");
        final var foreignCustomerId =
                insertCustomerDirectly(otherTenantId, "filter-foreign@example.com");
        final var matching =
                holdReservation(tenantId, resourceId, customerId, START_AT, END_AT)
                        .confirm(NOW.plusSeconds(30));
        final var wrongResource =
                holdReservation(
                                tenantId,
                                otherResourceId,
                                customerId,
                                START_AT.plusSeconds(3600),
                                END_AT.plusSeconds(3600))
                        .confirm(NOW.plusSeconds(30));
        final var wrongCustomer =
                holdReservation(
                                tenantId,
                                resourceId,
                                otherCustomerId,
                                START_AT.plusSeconds(7200),
                                END_AT.plusSeconds(7200))
                        .confirm(NOW.plusSeconds(30));
        final var foreignTenant =
                holdReservation(
                                otherTenantId,
                                foreignResourceId,
                                foreignCustomerId,
                                START_AT,
                                END_AT)
                        .confirm(NOW.plusSeconds(30));
        adapter.save(matching);
        adapter.save(wrongResource);
        adapter.save(wrongCustomer);
        adapter.save(foreignTenant);

        final var results =
                adapter.findByTenantIdBetweenWithFilters(
                        tenantId,
                        START_AT.minusSeconds(60),
                        END_AT.plusSeconds(3 * 3600L),
                        Optional.of(resourceId),
                        Optional.of(customerId),
                        Optional.of(ReservationStatus.CONFIRMED));

        assertEquals(1, results.size());
        assertEquals(matching.id(), results.getFirst().id());
    }

    @Test
    void noShowReleasesSlotAndCheckInBlocksSlot() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "reservation-slot-state");
        final var resourceId =
                insertResourceDirectly(jdbcTemplate, NOW, tenantId, "slot-state-room");
        final var customerId = insertCustomerDirectly(tenantId, "slot-state@example.com");
        final var noShow =
                holdReservation(tenantId, resourceId, customerId, START_AT, END_AT)
                        .confirm(NOW.plusSeconds(30))
                        .markNoShow(END_AT);
        final var checkedIn =
                holdReservation(
                                tenantId,
                                resourceId,
                                customerId,
                                START_AT.plusSeconds(3600),
                                END_AT.plusSeconds(3600))
                        .confirm(NOW.plusSeconds(30))
                        .checkIn(START_AT.plusSeconds(3600));
        adapter.save(noShow);
        adapter.save(checkedIn);

        assertFalse(adapter.existsActiveOverlap(tenantId, resourceId, START_AT, END_AT));
        assertTrue(
                adapter.existsActiveOverlap(
                        tenantId,
                        resourceId,
                        START_AT.plusSeconds(3600),
                        END_AT.plusSeconds(3600)));
    }

    private CustomerId insertCustomerDirectly(final TenantId tenantId, final String email) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO customer (id, tenant_id, email, name, hashed_password, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                tenantId.value(),
                email,
                "Customer",
                "$argon2id$v=19$m=65536,t=3,p=1$test",
                true,
                Timestamp.from(NOW));
        return CustomerId.of(id);
    }

    private static Reservation holdReservation(
            final TenantId tenantId,
            final ResourceId resourceId,
            final CustomerId customerId,
            final Instant startAt,
            final Instant endAt) {
        return Reservation.hold(
                tenantId, resourceId, customerId, startAt, endAt, NOW.plusSeconds(300), NOW);
    }
}
