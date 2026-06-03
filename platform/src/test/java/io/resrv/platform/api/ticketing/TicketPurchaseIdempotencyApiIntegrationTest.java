package io.resrv.platform.api;

import static io.resrv.platform.api.TicketingApiTestSupport.bearer;
import static io.resrv.platform.api.TicketingApiTestSupport.clean;
import static io.resrv.platform.api.TicketingApiTestSupport.insertAccount;
import static io.resrv.platform.api.TicketingApiTestSupport.insertBusiness;
import static io.resrv.platform.api.TicketingApiTestSupport.insertEvent;
import static io.resrv.platform.api.TicketingApiTestSupport.insertSeat;
import static io.resrv.platform.api.TicketingApiTestSupport.purchaseBody;
import static io.resrv.platform.api.TicketingApiTestSupport.submitPurchase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:tc:postgresql:16:///resrv",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "resrv.jwt.secret-key=01234567890123456789012345678901",
            "resrv.jwt.issuer=resrv-test",
            "resrv.jwt.audience=resrv-api",
            "resrv.jwt.expiration=3600",
            "resrv.security.password-reset.public-base-url=https://app.example.com",
            "resrv.security.password-reset.token-ttl=PT30M"
        })
@AutoConfigureMockMvc
@Import(FakePasswordResetEmailAdapter.class)
final class TicketPurchaseIdempotencyApiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clean(jdbcTemplate);
    }

    @Test
    void requiresKeyReplaysSameRequestRejectsChangedAndExpiredKeyAndScopesCustomers()
            throws Exception {
        final var customerId = insertAccount(jdbcTemplate, "customer@example.com");
        final var otherCustomerId = insertAccount(jdbcTemplate, "other@example.com");
        final var businessId = insertBusiness(jdbcTemplate, "ticketing-idempotency");
        final var eventId = insertEvent(jdbcTemplate, businessId);
        final var firstSeatId = insertSeat(jdbcTemplate, eventId, "A-1");
        final var secondSeatId = insertSeat(jdbcTemplate, eventId, "A-2");

        mockMvc.perform(
                        post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(customerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"seatIds\":[\"%s\"]}".formatted(firstSeatId)))
                .andExpect(status().isBadRequest());

        final var firstResponse =
                mockMvc.perform(
                                post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(customerId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(purchaseBody("purchase-key", firstSeatId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final String purchaseId = JsonPath.read(firstResponse, "$.id");

        for (int index = 0; index < 10; index++) {
            mockMvc.perform(
                            post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                    .header(HttpHeaders.AUTHORIZATION, bearer(customerId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(purchaseBody("purchase-key", firstSeatId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(purchaseId));
        }

        mockMvc.perform(
                        post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(customerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(purchaseBody("purchase-key", secondSeatId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("invalid_retry"));

        jdbcTemplate.update(
                """
                UPDATE ticketing.ticket_purchase_idempotency
                SET created_at = ?, expires_at = ?, cleanup_eligible_at = ?
                WHERE customer_account_id = ? AND idempotency_key = 'purchase-key'
                """,
                Timestamp.from(Instant.now().minusSeconds(172_800)),
                Timestamp.from(Instant.now().minusSeconds(86_400)),
                Timestamp.from(Instant.now().plusSeconds(2_592_000)),
                customerId);

        mockMvc.perform(
                        post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(customerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(purchaseBody("purchase-key", firstSeatId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("expired_key"));

        mockMvc.perform(
                        post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(otherCustomerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(purchaseBody("purchase-key", secondSeatId)))
                .andExpect(status().isCreated());

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM ticketing.ticket_purchase", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void simultaneousSameCustomerSameKeyRequestsReplayOneStableOutcome() throws Exception {
        final var customerId = insertAccount(jdbcTemplate, "same-key@example.com");
        final var businessId = insertBusiness(jdbcTemplate, "ticketing-same-key");
        final var eventId = insertEvent(jdbcTemplate, businessId);
        final var seatId = insertSeat(jdbcTemplate, eventId, "A-1");

        try (final var executor = Executors.newFixedThreadPool(8)) {
            final var futures =
                    IntStream.range(0, 10)
                            .mapToObj(
                                    index ->
                                            executor.submit(
                                                    () ->
                                                            submitPurchase(
                                                                    mockMvc,
                                                                    customerId,
                                                                    eventId,
                                                                    "same-key",
                                                                    seatId)))
                            .toList();
            final var statuses = new ArrayList<Integer>();
            for (final var future : futures) {
                statuses.add(future.get());
            }

            assertThat(statuses).containsOnly(201);
            assertThat(
                            jdbcTemplate.queryForObject(
                                    "SELECT COUNT(*) FROM ticketing.ticket_purchase",
                                    Integer.class))
                    .isEqualTo(1);
            assertThat(
                            jdbcTemplate.queryForObject(
                                    "SELECT COUNT(*) FROM ticketing.ticket_purchase_idempotency",
                                    Integer.class))
                    .isEqualTo(1);
        }
    }
}
