package io.resrv.platform.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
final class TicketingPurchaseOpenApiIntegrationTest {

    private static final String CONFIRM_PURCHASE_PATH =
            "$.paths['/api/ticketing/events/{ticketEventId}/purchases'].post";
    private static final String CONFIRM_PURCHASE_REQUEST_SCHEMA =
            "$.components.schemas.ConfirmTicketPurchaseRequest";
    private static final String PURCHASE_RESPONSE_SCHEMA =
            "$.components.schemas.TicketPurchaseResponse";
    private static final String CUSTOMER_HISTORY_SCHEMA =
            "$.components.schemas.CustomerTicketHistoryResponse";
    private static final String BUSINESS_ACTIVITY_SCHEMA =
            "$.components.schemas.BusinessTicketActivityResponse";

    @Autowired private MockMvc mockMvc;

    @Test
    void generatedOpenApiDocumentsTicketingPurchaseEndpointsAndNoAttemptEndpoints()
            throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(CONFIRM_PURCHASE_PATH + ".summary")
                                .value("Confirm selected-seat ticket purchase"))
                .andExpect(
                        jsonPath(CONFIRM_PURCHASE_REQUEST_SCHEMA + ".required")
                                .value(Matchers.hasItem("seatIds")))
                .andExpect(
                        jsonPath(CONFIRM_PURCHASE_REQUEST_SCHEMA + ".required")
                                .value(Matchers.hasItem("idempotencyKey")))
                .andExpect(
                        jsonPath(CONFIRM_PURCHASE_REQUEST_SCHEMA + ".properties.seatIds.minItems")
                                .value(1))
                .andExpect(
                        jsonPath(
                                        CONFIRM_PURCHASE_REQUEST_SCHEMA
                                                + ".properties.idempotencyKey.maxLength")
                                .value(120))
                .andExpect(
                        jsonPath(CONFIRM_PURCHASE_PATH + ".responses.409.description")
                                .value(Matchers.containsString("Unavailable selected seats")))
                .andExpect(
                        jsonPath(CONFIRM_PURCHASE_PATH + ".responses.400.description")
                                .value(Matchers.containsString("invalid_retry")))
                .andExpect(
                        jsonPath(CONFIRM_PURCHASE_PATH + ".responses.400.description")
                                .value(Matchers.containsString("expired_key")))
                .andExpect(jsonPath(PURCHASE_RESPONSE_SCHEMA + ".properties.outcome").exists())
                .andExpect(jsonPath(PURCHASE_RESPONSE_SCHEMA + ".properties.id").exists())
                .andExpect(
                        jsonPath(PURCHASE_RESPONSE_SCHEMA + ".properties.ticketEventId").exists())
                .andExpect(
                        jsonPath(PURCHASE_RESPONSE_SCHEMA + ".properties.customerAccountId")
                                .exists())
                .andExpect(jsonPath(PURCHASE_RESPONSE_SCHEMA + ".properties.seatIds").exists())
                .andExpect(jsonPath(PURCHASE_RESPONSE_SCHEMA + ".properties.confirmedAt").exists())
                .andExpect(
                        jsonPath("$.paths['/api/ticketing/customers/me/purchases'].get.summary")
                                .value("List customer ticket history"))
                .andExpect(jsonPath(CUSTOMER_HISTORY_SCHEMA + ".properties.items").exists())
                .andExpect(
                        jsonPath("$.components.schemas.CustomerTicketHistoryResponseItem").exists())
                .andExpect(jsonPath("$.components.schemas.TicketSeatResponse").exists())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/ticketing/business/events/"
                                                + "{ticketEventId}/purchases'].get.summary")
                                .value("List business ticket purchase activity"))
                .andExpect(
                        jsonPath(BUSINESS_ACTIVITY_SCHEMA + ".properties.ticketEventId").exists())
                .andExpect(jsonPath(BUSINESS_ACTIVITY_SCHEMA + ".properties.items").exists())
                .andExpect(jsonPath("$.paths['/api/ticketing/checkout']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/ticketing/attempts']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/ticketing/holds']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/ticketing/payments']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/ticketing/queues']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/ticketing/waitlist']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/ticketing/transfers']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/ticketing/resale']").doesNotExist());
    }
}
