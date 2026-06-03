package io.resrv.platform.api;

import static io.resrv.platform.api.TicketingApiTestSupport.bearer;
import static io.resrv.platform.api.TicketingApiTestSupport.clean;
import static io.resrv.platform.api.TicketingApiTestSupport.insertAccount;
import static io.resrv.platform.api.TicketingApiTestSupport.insertBusiness;
import static io.resrv.platform.api.TicketingApiTestSupport.insertEvent;
import static io.resrv.platform.api.TicketingApiTestSupport.insertSeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
final class TicketPurchaseApiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clean(jdbcTemplate);
    }

    @Test
    void customerCanConfirmPurchaseRetrySamePurchaseAndOtherCustomerGetsUnavailable()
            throws Exception {
        final var customerId = insertAccount(jdbcTemplate, "customer@example.com");
        final var otherCustomerId = insertAccount(jdbcTemplate, "other@example.com");
        final var businessId = insertBusiness(jdbcTemplate, "ticketing");
        final var eventId = insertEvent(jdbcTemplate, businessId);
        final var firstSeatId = insertSeat(jdbcTemplate, eventId, "A-1");
        final var secondSeatId = insertSeat(jdbcTemplate, eventId, "A-2");
        final var body = "{\"seatIds\":[\"%s\",\"%s\"]}".formatted(firstSeatId, secondSeatId);

        final var purchaseResponse =
                mockMvc.perform(
                                post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(customerId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.customerAccountId").value(customerId.toString()))
                        .andExpect(jsonPath("$.seatIds[0]").value(firstSeatId.toString()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final String purchaseId = JsonPath.read(purchaseResponse, "$.id");

        mockMvc.perform(
                        post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(customerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(purchaseId));

        mockMvc.perform(
                        post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(otherCustomerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM ticketing.ticket_purchase", Integer.class))
                .isEqualTo(1);
    }
}
