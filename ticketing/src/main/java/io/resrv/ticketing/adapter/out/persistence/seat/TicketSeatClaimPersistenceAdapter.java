package io.resrv.ticketing.adapter.out.persistence.seat;

import io.resrv.ticketing.application.seat.out.TicketSeatClaimPort;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class TicketSeatClaimPersistenceAdapter implements TicketSeatClaimPort {

    private final JdbcTemplate jdbcTemplate;

    TicketSeatClaimPersistenceAdapter(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean claimAvailableSeats(final TicketPurchase purchase) {
        final var orderedSeatIds =
                purchase.seatIds().stream().map(TicketSeatId::value).sorted().toList();
        final var placeholders =
                orderedSeatIds.stream()
                        .map(ignored -> "?::uuid")
                        .reduce((l, r) -> l + "," + r)
                        .orElseThrow();
        final var sql =
                """
                WITH requested(id) AS (
                    SELECT * FROM unnest(ARRAY[%s])
                ),
                available AS (
                    SELECT s.id
                    FROM ticketing.ticket_seat s
                    JOIN requested r ON r.id = s.id
                    WHERE s.ticket_event_id = ?
                      AND s.status = 'AVAILABLE'
                    ORDER BY s.id
                    FOR UPDATE OF s
                ),
                inserted_purchase AS (
                    INSERT INTO ticketing.ticket_purchase (
                        id, ticket_event_id, customer_account_id, confirmed_at
                    )
                    SELECT ?, ?, ?, ?
                    WHERE (SELECT COUNT(*) FROM available) = ?
                    RETURNING id
                ),
                claimed AS (
                    UPDATE ticketing.ticket_seat s
                    SET status = 'PURCHASED',
                        purchased_at = ?,
                        purchase_id = (SELECT id FROM inserted_purchase)
                    FROM available a
                    WHERE s.id = a.id
                      AND EXISTS (SELECT 1 FROM inserted_purchase)
                    RETURNING s.id
                )
                INSERT INTO ticketing.ticket_purchase_seat (ticket_purchase_id, ticket_seat_id)
                SELECT (SELECT id FROM inserted_purchase), id
                FROM claimed
                """
                        .formatted(placeholders);
        final var parameters = parameters(purchase, orderedSeatIds);
        return jdbcTemplate.update(sql, parameters.toArray()) == orderedSeatIds.size();
    }

    private static List<Object> parameters(
            final TicketPurchase purchase, final List<UUID> orderedSeatIds) {
        final var parameters = new ArrayList<Object>();
        parameters.addAll(orderedSeatIds);
        parameters.add(purchase.ticketEventId().value());
        parameters.add(purchase.id().value());
        parameters.add(purchase.ticketEventId().value());
        parameters.add(purchase.customerAccountId().value());
        parameters.add(Timestamp.from(purchase.confirmedAt()));
        parameters.add(orderedSeatIds.size());
        parameters.add(Timestamp.from(purchase.confirmedAt()));
        return parameters;
    }
}
