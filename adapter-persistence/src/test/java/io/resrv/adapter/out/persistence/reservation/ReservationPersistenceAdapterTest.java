package io.resrv.adapter.out.persistence.reservation;

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
        final var tenantId = insertTenantDirectly("reservation-save");
        final var resourceId = insertResourceDirectly(tenantId, "save-room");
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
        final var tenantId = insertTenantDirectly("reservation-overlap");
        final var resourceId = insertResourceDirectly(tenantId, "overlap-room");
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
        final var tenantId = insertTenantDirectly("reservation-expire");
        final var resourceId = insertResourceDirectly(tenantId, "expire-room");
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
        final var tenantId = insertTenantDirectly("reservation-cancelled");
        final var resourceId = insertResourceDirectly(tenantId, "cancelled-room");
        final var customerId = insertCustomerDirectly(tenantId, "cancelled@example.com");
        final var cancelled =
                holdReservation(tenantId, resourceId, customerId, START_AT, END_AT)
                        .cancelByAdmin(NOW.plusSeconds(30));

        adapter.save(cancelled);

        assertFalse(adapter.existsActiveOverlap(tenantId, resourceId, START_AT, END_AT));
    }

    private TenantId insertTenantDirectly(final String slugPrefix) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO tenant (id, name, slug, timezone, slot_duration, hold_ttl, cancellation_window, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "Test Tenant",
                slugPrefix + "-" + Math.abs(System.nanoTime()),
                "UTC",
                30,
                5,
                0,
                Timestamp.from(NOW));
        return TenantId.of(id);
    }

    private ResourceId insertResourceDirectly(final TenantId tenantId, final String slug) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO resource (id, tenant_id, slug, name, description, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                tenantId.value(),
                slug,
                "Room " + slug,
                "Quiet",
                "ACTIVE",
                Timestamp.from(NOW),
                Timestamp.from(NOW));
        return ResourceId.of(id);
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
