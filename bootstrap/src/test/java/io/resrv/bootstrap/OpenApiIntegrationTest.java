package io.resrv.bootstrap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void openApiJsonIsPublicAndDocumentsCurrentApiSurface() throws Exception {
        mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Resrv Reservation API"))
                .andExpect(jsonPath("$.paths['/api/tenants']").exists())
                .andExpect(jsonPath("$.paths['/public/{tenantSlug}/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/me']").exists())
                .andExpect(jsonPath("$.paths['/api/resources']").exists())
                .andExpect(jsonPath("$.paths['/api/resources/{resourceId}']").exists())
                .andExpect(jsonPath("$.paths['/public/{tenantSlug}/customers']").exists())
                .andExpect(jsonPath("$.paths['/public/{tenantSlug}/customers/login']").exists())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/resources/{resourceId}/weekly-availability/{dayOfWeek}']")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/resources/{resourceId}/availability-exceptions/{date}']")
                                .exists())
                .andExpect(jsonPath("$.paths['/api/resources/{resourceId}/slots']").exists())
                .andExpect(jsonPath("$.paths['/api/resources/{resourceId}/reservations']").exists())
                .andExpect(jsonPath("$.paths['/api/reservation-holds']").exists())
                .andExpect(
                        jsonPath("$.paths['/api/reservation-holds/{reservationId}/confirm']")
                                .exists())
                .andExpect(jsonPath("$.paths['/api/me/reservations']").exists())
                .andExpect(
                        jsonPath("$.paths['/api/me/reservations/{reservationId}/cancel']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/")));
    }
}
