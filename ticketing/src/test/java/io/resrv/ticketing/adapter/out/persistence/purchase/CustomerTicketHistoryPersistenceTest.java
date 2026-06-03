package io.resrv.ticketing.adapter.out.persistence.purchase;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerTicketHistoryPersistenceTest {

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void projectsCustomerPurchasesWithEventAndSeatDetails() {
        final var adapter = new TicketPurchaseActivityPersistenceAdapter(jdbcTemplate);
        final var customerId = AccountId.create();
        final var otherCustomerId = AccountId.create();
        final var eventId = insertEvent("Concert");
        insertPurchase(eventId, customerId.value(), "A-1");
        insertPurchase(eventId, otherCustomerId.value(), "B-1");

        final var views = adapter.findCustomerPurchases(customerId);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().eventTitle()).isEqualTo("Concert");
        assertThat(views.getFirst().seats().getFirst().displayLabel()).isEqualTo("A-1");
    }

    private UUID insertEvent(final String title) {
        final var eventId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO ticketing.ticket_event (
                    id, business_id, title, event_start_at, event_end_at, event_timezone,
                    sale_start_at, sale_end_at, sale_timezone, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                """,
                eventId,
                BusinessId.create().value(),
                title,
                Timestamp.from(Instant.parse("2026-06-04T00:00:00Z")),
                Timestamp.from(Instant.parse("2026-06-04T02:00:00Z")),
                Timezone.of("Asia/Seoul").value().getId(),
                Timestamp.from(Instant.parse("2026-06-01T00:00:00Z")),
                Timestamp.from(Instant.parse("2026-06-03T01:00:00Z")),
                Timezone.of("Asia/Seoul").value().getId(),
                Timestamp.from(Instant.parse("2026-06-03T00:00:00Z")),
                Timestamp.from(Instant.parse("2026-06-03T00:00:00Z")));
        return eventId;
    }

    private void insertPurchase(final UUID eventId, final UUID customerId, final String label) {
        final var purchaseId = UUID.randomUUID();
        final var seatId = UUID.randomUUID();
        final var now = Timestamp.from(Instant.parse("2026-06-03T00:00:00Z"));
        jdbcTemplate.update(
                """
                INSERT INTO ticketing.ticket_purchase (
                    id, ticket_event_id, customer_account_id, confirmed_at
                ) VALUES (?, ?, ?, ?)
                """,
                purchaseId,
                eventId,
                customerId,
                now);
        jdbcTemplate.update(
                """
                INSERT INTO ticketing.ticket_seat (
                    id, ticket_event_id, display_label, status, purchased_at, purchase_id
                ) VALUES (?, ?, ?, 'PURCHASED', ?, ?)
                """,
                seatId,
                eventId,
                label,
                now,
                purchaseId);
        jdbcTemplate.update(
                "INSERT INTO ticketing.ticket_purchase_seat (ticket_purchase_id, ticket_seat_id) VALUES (?, ?)",
                purchaseId,
                seatId);
    }
}
