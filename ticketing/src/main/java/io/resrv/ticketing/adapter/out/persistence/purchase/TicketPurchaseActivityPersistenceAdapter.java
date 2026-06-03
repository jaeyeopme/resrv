package io.resrv.ticketing.adapter.out.persistence.purchase;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.ticketing.application.activity.out.TicketPurchaseActivityQueryPort;
import io.resrv.ticketing.domain.event.TicketEventId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class TicketPurchaseActivityPersistenceAdapter implements TicketPurchaseActivityQueryPort {

    private final JdbcTemplate jdbcTemplate;

    TicketPurchaseActivityPersistenceAdapter(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CustomerPurchaseView> findCustomerPurchases(final AccountId customerAccountId) {
        final var rows =
                jdbcTemplate.query(
                        """
                        SELECT p.id AS purchase_id,
                               p.ticket_event_id,
                               e.title AS event_title,
                               e.event_start_at,
                               s.id AS seat_id,
                               s.display_label,
                               p.confirmed_at
                        FROM ticketing.ticket_purchase p
                        JOIN ticketing.ticket_event e ON e.id = p.ticket_event_id
                        JOIN ticketing.ticket_purchase_seat ps ON ps.ticket_purchase_id = p.id
                        JOIN ticketing.ticket_seat s ON s.id = ps.ticket_seat_id
                        WHERE p.customer_account_id = ?
                        ORDER BY p.confirmed_at DESC, s.display_label ASC
                        """,
                        (rs, rowNum) -> customerRow(rs),
                        customerAccountId.value());
        final var grouped = new LinkedHashMap<UUID, CustomerPurchaseBuilder>();
        for (final var row : rows) {
            grouped.computeIfAbsent(row.purchaseId(), id -> row.toBuilder()).addSeat(row.seat());
        }
        return grouped.values().stream().map(CustomerPurchaseBuilder::build).toList();
    }

    @Override
    public List<BusinessPurchaseView> findBusinessEventPurchases(
            final BusinessId businessId, final TicketEventId ticketEventId) {
        final var rows =
                jdbcTemplate.query(
                        """
                        SELECT p.id AS purchase_id,
                               p.ticket_event_id,
                               e.title AS event_title,
                               p.customer_account_id,
                               s.id AS seat_id,
                               s.display_label,
                               p.confirmed_at
                        FROM ticketing.ticket_purchase p
                        JOIN ticketing.ticket_event e ON e.id = p.ticket_event_id
                        JOIN ticketing.ticket_purchase_seat ps ON ps.ticket_purchase_id = p.id
                        JOIN ticketing.ticket_seat s ON s.id = ps.ticket_seat_id
                        WHERE e.business_id = ? AND p.ticket_event_id = ?
                        ORDER BY p.confirmed_at DESC, s.display_label ASC
                        """,
                        (rs, rowNum) -> businessRow(rs),
                        businessId.value(),
                        ticketEventId.value());
        final var grouped = new LinkedHashMap<UUID, BusinessPurchaseBuilder>();
        for (final var row : rows) {
            grouped.computeIfAbsent(row.purchaseId(), id -> row.toBuilder()).addSeat(row.seat());
        }
        return grouped.values().stream().map(BusinessPurchaseBuilder::build).toList();
    }

    private static CustomerRow customerRow(final ResultSet rs) throws SQLException {
        return new CustomerRow(
                rs.getObject("purchase_id", UUID.class),
                rs.getObject("ticket_event_id", UUID.class),
                rs.getString("event_title"),
                rs.getTimestamp("event_start_at").toInstant(),
                seat(rs),
                rs.getTimestamp("confirmed_at").toInstant());
    }

    private static BusinessRow businessRow(final ResultSet rs) throws SQLException {
        return new BusinessRow(
                rs.getObject("purchase_id", UUID.class),
                rs.getObject("ticket_event_id", UUID.class),
                rs.getString("event_title"),
                rs.getObject("customer_account_id", UUID.class),
                seat(rs),
                rs.getTimestamp("confirmed_at").toInstant());
    }

    private static SeatView seat(final ResultSet rs) throws SQLException {
        return new SeatView(rs.getObject("seat_id", UUID.class), rs.getString("display_label"));
    }

    private record CustomerRow(
            UUID purchaseId,
            UUID ticketEventId,
            String eventTitle,
            Instant eventStartAt,
            SeatView seat,
            Instant confirmedAt) {

        CustomerPurchaseBuilder toBuilder() {
            return new CustomerPurchaseBuilder(
                    purchaseId, ticketEventId, eventTitle, eventStartAt, confirmedAt);
        }
    }

    private record BusinessRow(
            UUID purchaseId,
            UUID ticketEventId,
            String eventTitle,
            UUID customerAccountId,
            SeatView seat,
            Instant confirmedAt) {

        BusinessPurchaseBuilder toBuilder() {
            return new BusinessPurchaseBuilder(
                    purchaseId, ticketEventId, eventTitle, customerAccountId, confirmedAt);
        }
    }

    private record CustomerPurchaseBuilder(
            UUID purchaseId,
            UUID ticketEventId,
            String eventTitle,
            Instant eventStartAt,
            Instant confirmedAt,
            List<SeatView> seats) {

        CustomerPurchaseBuilder(
                final UUID purchaseId,
                final UUID ticketEventId,
                final String eventTitle,
                final Instant eventStartAt,
                final Instant confirmedAt) {
            this(
                    purchaseId,
                    ticketEventId,
                    eventTitle,
                    eventStartAt,
                    confirmedAt,
                    new ArrayList<>());
        }

        void addSeat(final SeatView seat) {
            seats.add(seat);
        }

        CustomerPurchaseView build() {
            return new CustomerPurchaseView(
                    purchaseId,
                    ticketEventId,
                    eventTitle,
                    eventStartAt,
                    List.copyOf(seats),
                    confirmedAt);
        }
    }

    private record BusinessPurchaseBuilder(
            UUID purchaseId,
            UUID ticketEventId,
            String eventTitle,
            UUID customerAccountId,
            Instant confirmedAt,
            List<SeatView> seats) {

        BusinessPurchaseBuilder(
                final UUID purchaseId,
                final UUID ticketEventId,
                final String eventTitle,
                final UUID customerAccountId,
                final Instant confirmedAt) {
            this(
                    purchaseId,
                    ticketEventId,
                    eventTitle,
                    customerAccountId,
                    confirmedAt,
                    new ArrayList<>());
        }

        void addSeat(final SeatView seat) {
            seats.add(seat);
        }

        BusinessPurchaseView build() {
            return new BusinessPurchaseView(
                    purchaseId,
                    ticketEventId,
                    eventTitle,
                    customerAccountId,
                    List.copyOf(seats),
                    confirmedAt);
        }
    }
}
