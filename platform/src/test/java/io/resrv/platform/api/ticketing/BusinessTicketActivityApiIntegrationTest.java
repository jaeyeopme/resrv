package io.resrv.platform.api;

import static io.resrv.platform.api.TicketingApiTestSupport.bearer;
import static io.resrv.platform.api.TicketingApiTestSupport.clean;
import static io.resrv.platform.api.TicketingApiTestSupport.insertAccount;
import static io.resrv.platform.api.TicketingApiTestSupport.insertBusiness;
import static io.resrv.platform.api.TicketingApiTestSupport.insertEvent;
import static io.resrv.platform.api.TicketingApiTestSupport.insertMembership;
import static io.resrv.platform.api.TicketingApiTestSupport.insertSeat;
import static io.resrv.platform.api.TicketingApiTestSupport.purchaseBody;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
final class BusinessTicketActivityApiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clean(jdbcTemplate);
    }

    @Test
    void authorizedBusinessActorCanReviewPurchasesAndUnauthorizedActorGetsNotFound()
            throws Exception {
        final var ownerId = insertAccount(jdbcTemplate, "owner@example.com");
        final var customerId = insertAccount(jdbcTemplate, "customer@example.com");
        final var outsiderId = insertAccount(jdbcTemplate, "outsider@example.com");
        final var businessId = insertBusiness(jdbcTemplate, "ticketing-business");
        insertMembership(jdbcTemplate, ownerId, businessId, "OWNER");
        final var eventId = insertEvent(jdbcTemplate, businessId);
        final var seatId = insertSeat(jdbcTemplate, eventId, "A-1");
        mockMvc.perform(
                        post("/api/ticketing/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(customerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(purchaseBody("business-activity-key", seatId)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/ticketing/business/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].customerAccountId").value(customerId.toString()))
                .andExpect(jsonPath("$.items[0].seats[0].displayLabel").value("A-1"));

        mockMvc.perform(
                        get("/api/ticketing/business/events/{ticketEventId}/purchases", eventId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(outsiderId)))
                .andExpect(status().isNotFound());
    }
}
