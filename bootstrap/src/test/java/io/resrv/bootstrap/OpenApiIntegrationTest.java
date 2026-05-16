package io.resrv.bootstrap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class OpenApiIntegrationTest extends AbstractIntegrationTest {

    private static final Set<String> EXPECTED_PATH_VERBS =
            Set.of(
                    "POST /api/tenants",
                    "POST /public/{tenantSlug}/auth/login",
                    "POST /api/auth/logout",
                    "GET /api/auth/me",
                    "POST /api/resources",
                    "GET /api/resources",
                    "GET /api/resources/{resourceId}",
                    "PUT /api/resources/{resourceId}",
                    "DELETE /api/resources/{resourceId}",
                    "POST /public/{tenantSlug}/customers",
                    "POST /public/{tenantSlug}/customers/login",
                    "PUT /api/resources/{resourceId}/weekly-availability/{dayOfWeek}",
                    "DELETE /api/resources/{resourceId}/weekly-availability/{dayOfWeek}",
                    "PUT /api/resources/{resourceId}/availability-exceptions/{date}",
                    "DELETE /api/resources/{resourceId}/availability-exceptions/{date}",
                    "GET /api/resources/{resourceId}/slots",
                    "GET /api/resources/{resourceId}/reservations",
                    "GET /api/reservations",
                    "POST /api/reservations/{reservationId}/admin-cancel",
                    "POST /api/reservations/{reservationId}/check-in",
                    "POST /api/reservations/{reservationId}/no-show",
                    "POST /api/reservation-holds",
                    "POST /api/reservation-holds/{reservationId}/confirm",
                    "GET /api/me/reservations",
                    "POST /api/me/reservations/{reservationId}/cancel");
    private static final Set<String> DOCUMENTED_HTTP_METHODS =
            Set.of("get", "post", "put", "delete", "patch");

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                .andExpect(jsonPath("$.paths['/api/reservations']").exists())
                .andExpect(
                        jsonPath("$.paths['/api/reservations/{reservationId}/admin-cancel']")
                                .exists())
                .andExpect(
                        jsonPath("$.paths['/api/reservations/{reservationId}/check-in']").exists())
                .andExpect(
                        jsonPath("$.paths['/api/reservations/{reservationId}/no-show']").exists())
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
    void openApiDocumentsReviewerFacingOperationSemantics() throws Exception {
        mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/tenants'].post.tags[0]").value("Tenants"))
                .andExpect(
                        jsonPath("$.paths['/api/tenants'].post.summary").value("Create a tenant"))
                .andExpect(jsonPath("$.paths['/api/tenants'].post.responses.201").exists())
                .andExpect(jsonPath("$.paths['/api/tenants'].post.responses.400").exists())
                .andExpect(jsonPath("$.paths['/api/tenants'].post.responses.409").exists())
                .andExpect(
                        jsonPath("$.paths['/public/{tenantSlug}/auth/login'].post.summary")
                                .value("Log in as a tenant administrator"))
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post.responses.204").exists())
                .andExpect(
                        jsonPath("$.paths['/api/auth/logout'].post.security[0].bearerAuth")
                                .exists())
                .andExpect(jsonPath("$.paths['/api/resources'].post.tags[0]").value("Resources"))
                .andExpect(
                        jsonPath("$.paths['/api/resources'].post.summary")
                                .value("Create a resource"))
                .andExpect(jsonPath("$.paths['/api/resources'].post.responses.201").exists())
                .andExpect(jsonPath("$.paths['/api/resources'].post.responses.401").exists())
                .andExpect(jsonPath("$.paths['/api/resources'].post.responses.403").exists())
                .andExpect(jsonPath("$.paths['/api/resources'].post.responses.409").exists())
                .andExpect(
                        jsonPath("$.paths['/api/resources'].post.security[0].bearerAuth").exists())
                .andExpect(
                        jsonPath("$.paths['/api/resources/{resourceId}'].delete.responses.204")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/resources/{resourceId}'].get.parameters[0].description")
                                .value("Resource identifier."))
                .andExpect(
                        jsonPath("$.paths['/api/resources/{resourceId}/slots'].get.summary")
                                .value("List available slots"))
                .andExpect(
                        jsonPath("$.paths['/api/resources/{resourceId}/reservations'].get.tags[0]")
                                .value("Availability"))
                .andExpect(
                        jsonPath("$.paths['/api/reservations'].get.tags[0]")
                                .value("Admin Reservations"))
                .andExpect(
                        jsonPath("$.paths['/api/reservations'].get.parameters[0].required")
                                .value(true))
                .andExpect(
                        jsonPath("$.paths['/api/reservations'].get.parameters[1].required")
                                .value(false))
                .andExpect(
                        jsonPath("$.paths['/api/reservations'].get.parameters[2].required")
                                .value(false))
                .andExpect(
                        jsonPath("$.paths['/api/reservations'].get.parameters[3].required")
                                .value(false))
                .andExpect(jsonPath("$.paths['/api/reservations'].get.responses.200").exists())
                .andExpect(jsonPath("$.paths['/api/reservations'].get.responses.400").exists())
                .andExpect(
                        jsonPath("$.paths['/api/reservations'].get.security[0].bearerAuth")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/reservations/{reservationId}/admin-cancel'].post.responses.409")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/reservations/{reservationId}/check-in']"
                                                + ".post.security[0].bearerAuth")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/reservations/{reservationId}/no-show'].post.responses.200")
                                .exists())
                .andExpect(
                        jsonPath("$.paths['/api/reservation-holds'].post.tags[0]")
                                .value("Reservations"))
                .andExpect(
                        jsonPath("$.paths['/api/reservation-holds'].post.summary")
                                .value("Hold a reservation slot"))
                .andExpect(
                        jsonPath("$.paths['/api/reservation-holds'].post.responses.201").exists())
                .andExpect(
                        jsonPath("$.paths['/api/reservation-holds'].post.responses.409").exists())
                .andExpect(
                        jsonPath("$.paths['/api/reservation-holds'].post.security[0].bearerAuth")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.RegisterTenantRequest.properties.name.description")
                                .value("Tenant display name."))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.RegisterTenantRequest.properties.name.example")
                                .value("Demo Studio"))
                .andExpect(
                        jsonPath("$.components.schemas.LoginRequest.properties.password.writeOnly")
                                .value(true))
                .andExpect(
                        jsonPath("$.components.schemas.ResourceRequest.properties.slug.example")
                                .value("room-a"));
    }

    @Test
    void openApiContractDoesNotLeakRuntimeAdapterDetails() throws Exception {
        final var response =
                mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final JsonNode document = objectMapper.readTree(response);

        Assertions.assertFalse(
                response.contains("JwtAuthenticationToken"),
                "OpenAPI must not document Spring Security runtime parameters");
        Assertions.assertFalse(
                document.path("components").path("schemas").has("JwtAuthenticationToken"),
                "OpenAPI must not expose JwtAuthenticationToken as a schema");
        Assertions.assertEquals(EXPECTED_PATH_VERBS, pathVerbs(document));

        final JsonNode schemas = document.path("components").path("schemas");
        for (final var schemaName :
                Set.of(
                        "RegisterCustomerRequest",
                        "CustomerLoginRequest",
                        "CustomerResponse",
                        "WeeklyAvailabilityRequest",
                        "DateAvailabilityOverrideRequest",
                        "WeeklyAvailabilityResponse",
                        "DateAvailabilityOverrideResponse",
                        "SlotResponse",
                        "HoldReservationRequest")) {
            Assertions.assertTrue(
                    schemas.has(schemaName), () -> "Missing schema component " + schemaName);
        }
        Assertions.assertEquals(
                "Customer email within the tenant.",
                schemas.path("RegisterCustomerRequest")
                        .path("properties")
                        .path("email")
                        .path("description")
                        .asText());
        Assertions.assertEquals(
                "Resource identifier.",
                schemas.path("HoldReservationRequest")
                        .path("properties")
                        .path("resourceId")
                        .path("description")
                        .asText());
        Assertions.assertEquals(
                "Tenant-local opening time.",
                schemas.path("WeeklyAvailabilityRequest")
                        .path("properties")
                        .path("startTime")
                        .path("description")
                        .asText());
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/")));
    }

    private static Set<String> pathVerbs(final JsonNode document) {
        final Set<String> result = new TreeSet<>();
        document.path("paths")
                .properties()
                .forEach(
                        path ->
                                path.getValue()
                                        .fieldNames()
                                        .forEachRemaining(
                                                verb -> {
                                                    if (DOCUMENTED_HTTP_METHODS.contains(verb)) {
                                                        result.add(
                                                                verb.toUpperCase()
                                                                        + " "
                                                                        + path.getKey());
                                                    }
                                                }));
        return result;
    }
}
