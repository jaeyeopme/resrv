package io.resrv.platform.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Autowired private MockMvc mockMvc;

    @Test
    void generatedOpenApiDocumentsTicketingPurchaseEndpointsAndNoAttemptEndpoints()
            throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/ticketing/events/{ticketEventId}/purchases'].post.summary")
                                .value("Confirm selected-seat ticket purchase"))
                .andExpect(
                        jsonPath("$.paths['/api/ticketing/customers/me/purchases'].get.summary")
                                .value("List customer ticket history"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/ticketing/business/events/"
                                                + "{ticketEventId}/purchases'].get.summary")
                                .value("List business ticket purchase activity"))
                .andExpect(jsonPath("$.paths['/api/ticketing/checkout']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/ticketing/attempts']").doesNotExist());
    }
}
