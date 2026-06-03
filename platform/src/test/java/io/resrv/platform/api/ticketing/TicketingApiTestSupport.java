package io.resrv.platform.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

final class TicketingApiTestSupport {

    static final String JWT_SECRET = "01234567890123456789012345678901";
    static final String JWT_ISSUER = "resrv-test";
    static final String JWT_AUDIENCE = "resrv-api";
    static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    private TicketingApiTestSupport() {}

    static void clean(final JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM ticketing.ticket_purchase_idempotency");
        jdbcTemplate.update("DELETE FROM ticketing.ticket_purchase_seat");
        jdbcTemplate.update("DELETE FROM ticketing.ticket_seat");
        jdbcTemplate.update("DELETE FROM ticketing.ticket_purchase");
        jdbcTemplate.update("DELETE FROM ticketing.ticket_inventory_tier");
        jdbcTemplate.update("DELETE FROM ticketing.ticket_inventory");
        jdbcTemplate.update("DELETE FROM ticketing.ticket_event");
        jdbcTemplate.update("DELETE FROM platform.business_membership_audit_entry");
        jdbcTemplate.update("DELETE FROM platform.business_membership");
        jdbcTemplate.update("DELETE FROM platform.business");
        jdbcTemplate.update("DELETE FROM platform.account");
    }

    static UUID insertAccount(final JdbcTemplate jdbcTemplate, final String email) {
        final var accountId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO platform.account (
                    id, email, name, hashed_password, status, created_at
                ) VALUES (?, ?, ?, ?, 'ACTIVE', ?)
                """,
                accountId,
                email,
                email,
                "hash",
                Timestamp.from(NOW));
        return accountId;
    }

    static UUID insertBusiness(final JdbcTemplate jdbcTemplate, final String slug) {
        final var businessId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO platform.business (
                    id, name, slug, timezone, status, created_at
                ) VALUES (?, ?, ?, 'Asia/Seoul', 'ACTIVE', ?)
                """,
                businessId,
                slug,
                slug,
                Timestamp.from(NOW));
        return businessId;
    }

    static void insertMembership(
            final JdbcTemplate jdbcTemplate,
            final UUID accountId,
            final UUID businessId,
            final String role) {
        jdbcTemplate.update(
                """
                INSERT INTO platform.business_membership (
                    id, business_id, account_id, role, active, created_at, updated_at
                ) VALUES (?, ?, ?, ?, true, ?, ?)
                """,
                UUID.randomUUID(),
                businessId,
                accountId,
                role,
                Timestamp.from(NOW),
                Timestamp.from(NOW));
    }

    static UUID insertEvent(final JdbcTemplate jdbcTemplate, final UUID businessId) {
        final var eventId = UUID.randomUUID();
        final var now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO ticketing.ticket_event (
                    id, business_id, title, event_start_at, event_end_at, event_timezone,
                    sale_start_at, sale_end_at, sale_timezone, status, created_at, updated_at
                ) VALUES (?, ?, 'Concert', ?, ?, 'Asia/Seoul', ?, ?, 'Asia/Seoul', 'ACTIVE', ?, ?)
                """,
                eventId,
                businessId,
                Timestamp.from(Instant.parse("2026-06-04T00:00:00Z")),
                Timestamp.from(Instant.parse("2026-06-04T02:00:00Z")),
                Timestamp.from(now.minusSeconds(3600)),
                Timestamp.from(now.plusSeconds(3600)),
                Timestamp.from(now),
                Timestamp.from(now));
        return eventId;
    }

    static UUID insertSeat(
            final JdbcTemplate jdbcTemplate, final UUID eventId, final String label) {
        final var seatId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO ticketing.ticket_seat (
                    id, ticket_event_id, display_label, status, purchased_at, purchase_id
                ) VALUES (?, ?, ?, 'AVAILABLE', NULL, NULL)
                """,
                seatId,
                eventId,
                label);
        return seatId;
    }

    static String purchaseBody(final String idempotencyKey, final UUID... seatIds) {
        final var seats =
                Arrays.stream(seatIds)
                        .map(seatId -> "\"" + seatId + "\"")
                        .reduce((left, right) -> left + "," + right)
                        .orElse("");
        return "{\"seatIds\":[%s],\"idempotencyKey\":\"%s\"}".formatted(seats, idempotencyKey);
    }

    static int submitPurchase(
            final MockMvc mockMvc,
            final UUID customerId,
            final UUID eventId,
            final String idempotencyKey,
            final UUID... seatIds)
            throws Exception {
        return mockMvc.perform(
                        post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(customerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(purchaseBody(idempotencyKey, seatIds)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    static String bearer(final UUID accountId) throws JOSEException {
        final var issuedAt = Instant.now();
        final var claims =
                new JWTClaimsSet.Builder()
                        .jwtID(UUID.randomUUID().toString())
                        .subject(accountId.toString())
                        .claim("accountId", accountId.toString())
                        .issuer(JWT_ISSUER)
                        .audience(List.of(JWT_AUDIENCE))
                        .issueTime(Date.from(issuedAt))
                        .expirationTime(Date.from(issuedAt.plusSeconds(3600)))
                        .build();
        final var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(JWT_SECRET));
        return "Bearer " + jwt.serialize();
    }
}
