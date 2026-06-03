package io.resrv.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
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
final class PlatformRuntimePackagingIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901";
    private static final String JWT_ISSUER = "resrv-test";
    private static final String JWT_AUDIENCE = "resrv-api";
    private static final Instant NOW = Instant.parse("2026-05-29T00:00:00Z");
    private static final Pattern ENDPOINT_TABLE =
            Pattern.compile("(?m)^\\|\\s*(GET|POST|PUT|PATCH|DELETE)\\s+/api");

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM timeslot.reservation");
        jdbcTemplate.update("DELETE FROM timeslot.resource_date_schedule_override_window");
        jdbcTemplate.update("DELETE FROM timeslot.resource_date_schedule_override");
        jdbcTemplate.update("DELETE FROM timeslot.resource_weekly_schedule_window");
        jdbcTemplate.update("DELETE FROM timeslot.resource_weekly_schedule");
        jdbcTemplate.update("DELETE FROM timeslot.resource");
        jdbcTemplate.update("DELETE FROM timeslot.business_booking_settings");
        jdbcTemplate.update("DELETE FROM platform.sign_in_attempt");
        jdbcTemplate.update("DELETE FROM platform.password_reset_challenge");
        jdbcTemplate.update("DELETE FROM platform.account_sign_in_protection");
        jdbcTemplate.update("DELETE FROM platform.business_membership_audit_entry");
        jdbcTemplate.update("DELETE FROM platform.business_membership");
        jdbcTemplate.update("DELETE FROM platform.business");
        jdbcTemplate.update("DELETE FROM platform.account");
    }

    @Test
    void bookingSettingsEndpointIsServedFromPlatformRuntime() throws Exception {
        final var ownerId = insertAccount("owner@example.com", "Owner", "ACTIVE");
        final var businessId = insertBusiness("Salon A", "salon-a", "ACTIVE");
        insertMembership(ownerId, businessId, "OWNER", true);

        mockMvc.perform(
                        put("/api/businesses/{businessId}/booking-settings", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(settingsJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessId").value(businessId.toString()))
                .andExpect(jsonPath("$.slotDurationMinutes").value(30));
    }

    @Test
    void publicBookingDiscoveryEndpointIsServedFromPlatformRuntime() throws Exception {
        final var businessId = insertBusiness("Salon A", "salon-a", "ACTIVE");
        insertSettings(businessId);

        mockMvc.perform(get("/api/public/businesses/{businessSlug}", "salon-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("salon-a"))
                .andExpect(jsonPath("$.name").value("Salon A"))
                .andExpect(jsonPath("$.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.businessId").doesNotExist());
    }

    @Test
    void platformRuntimeMigratesPlatformAndTimeslotSchemas() {
        assertEquals("platform.account", regclass("platform.account"));
        assertEquals(
                "timeslot.business_booking_settings",
                regclass("timeslot.business_booking_settings"));
        assertEquals("ticketing.ticket_event", regclass("ticketing.ticket_event"));
    }

    @Test
    void protectedBookingEndpointRejectsInactiveAccounts() throws Exception {
        final var inactiveAccountId = insertAccount("inactive@example.com", "Inactive", "DISABLED");
        final var businessId = insertBusiness("Salon A", "salon-a", "ACTIVE");
        insertMembership(inactiveAccountId, businessId, "OWNER", true);

        mockMvc.perform(
                        put("/api/businesses/{businessId}/booking-settings", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(inactiveAccountId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(settingsJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongBusinessPublicSlotLookupKeepsNotFoundResponse() throws Exception {
        final var businessId = insertBusiness("Salon A", "salon-a", "ACTIVE");
        final var otherBusinessId = insertBusiness("Salon B", "salon-b", "ACTIVE");
        insertSettings(businessId);
        final var missingResourceId = UUID.fromString("00000000-0000-0000-0000-000000000034");
        final var otherResourceId = UUID.fromString("00000000-0000-0000-0000-000000000035");
        insertResource(otherBusinessId, otherResourceId, "Room B", "ACTIVE");

        final var missing =
                mockMvc.perform(
                                get(
                                                "/api/public/businesses/{businessSlug}/resources/{resourceId}/slots",
                                                "salon-a",
                                                missingResourceId)
                                        .param("date", "2026-05-29"))
                        .andExpect(status().isNotFound())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final var wrongBusiness =
                mockMvc.perform(
                                get(
                                                "/api/public/businesses/{businessSlug}/resources/{resourceId}/slots",
                                                "salon-a",
                                                otherResourceId)
                                        .param("date", "2026-05-29"))
                        .andExpect(status().isNotFound())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertEquals(
                (String) JsonPath.read(missing, "$.detail"),
                (String) JsonPath.read(wrongBusiness, "$.detail"));
    }

    @Test
    void generatedOpenApiIncludesPlatformAndBookingEndpointGroups() throws Exception {
        final var paths = openApiPaths(generatedOpenApi());

        assertOperation(paths, "/api/accounts", "post");
        assertOperation(paths, "/api/auth/login", "post");
        assertOperation(paths, "/api/auth/password-reset", "post");
        assertOperation(paths, "/api/businesses", "post");

        assertOperation(paths, "/api/businesses/{businessId}/memberships", "post");
        assertOperation(paths, "/api/businesses/{businessId}/memberships", "get");
        assertOperation(paths, "/api/businesses/{businessId}/memberships/audit", "get");
        assertOperation(paths, "/api/businesses/{businessId}/memberships/{membershipId}", "put");
        assertOperation(
                paths, "/api/businesses/{businessId}/memberships/{membershipId}/disable", "post");

        assertOperation(paths, "/api/businesses/{businessId}/booking-settings", "put");
        assertOperation(paths, "/api/businesses/{businessId}/resources", "post");
        assertOperation(paths, "/api/businesses/{businessId}/resources", "get");
        assertOperation(paths, "/api/businesses/{businessId}/resources/{resourceId}", "put");
        assertOperation(
                paths, "/api/businesses/{businessId}/resources/{resourceId}/activate", "post");
        assertOperation(
                paths, "/api/businesses/{businessId}/resources/{resourceId}/deactivate", "post");
        assertOperation(
                paths,
                "/api/businesses/{businessId}/resources/{resourceId}/weekly-schedules/{dayOfWeek}",
                "put");
        assertOperation(
                paths,
                "/api/businesses/{businessId}/resources/{resourceId}/date-schedule-overrides/{date}",
                "put");
        assertOperation(paths, "/api/businesses/{businessId}/resources/{resourceId}/slots", "get");

        assertOperation(paths, "/api/businesses/{businessId}/reservations", "post");
        assertOperation(paths, "/api/businesses/{businessId}/reservations", "get");
        assertOperation(
                paths, "/api/businesses/{businessId}/reservations/{reservationId}/confirm", "post");
        assertOperation(
                paths, "/api/businesses/{businessId}/reservations/{reservationId}/release", "post");
        assertOperation(
                paths, "/api/businesses/{businessId}/reservations/{reservationId}/cancel", "post");
        assertOperation(
                paths,
                "/api/businesses/{businessId}/reservations/{reservationId}/check-in",
                "post");
        assertOperation(
                paths, "/api/businesses/{businessId}/reservations/{reservationId}/no-show", "post");

        assertOperation(paths, "/api/me/reservations", "get");
        assertOperation(paths, "/api/me/reservations/{reservationId}", "get");

        assertOperation(paths, "/api/public/businesses/{businessSlug}", "get");
        assertOperation(paths, "/api/public/businesses/{businessSlug}/resources", "get");
        assertOperation(
                paths, "/api/public/businesses/{businessSlug}/resources/{resourceId}/slots", "get");
        assertOperation(paths, "/api/public/businesses/{businessSlug}/reservations", "post");
    }

    @Test
    void generatedOpenApiExcludesUnsupportedCapabilityGroups() throws Exception {
        final var paths = openApiPaths(generatedOpenApi());

        assertNoPathContaining(paths, "payment");
        assertNoPathContaining(paths, "notification");
        assertNoPathContaining(paths, "calendar");
        assertNoPathContaining(paths, "broker");
        assertNoPathContaining(paths, "outbox");
        assertNoPathContaining(paths, "/api/events");
        assertNoPathContaining(paths, "ticket");
    }

    @Test
    void generatedOpenApiDocumentsPublicAndProtectedBoundarySchemas() throws Exception {
        final var openApi = generatedOpenApi();

        assertSchemaHasProperties(openApi, "PublicBusinessResponse", "slug", "name", "timezone");
        assertSchemaOmitsProperties(openApi, "PublicBusinessResponse", "id", "businessId");
        assertSchemaHasProperties(
                openApi, "PublicResourceResponse", "resourceId", "businessSlug", "name");
        assertSchemaOmitsProperties(
                openApi, "PublicResourceResponse", "businessId", "slug", "handle");
        assertSchemaHasProperties(
                openApi, "PublicReservationResponse", "id", "resourceId", "state");
        assertSchemaOmitsProperties(
                openApi, "PublicReservationResponse", "businessId", "customerAccountId");

        assertSchemaHasProperties(
                openApi, "ReservationResponse", "businessId", "resourceId", "customerAccountId");
        assertSchemaHasProperties(
                openApi, "MembershipResponse", "businessId", "account", "role", "active");
        assertSchemaHasProperties(
                openApi, "CustomerReservationResponse", "business", "resource", "state");
    }

    @Test
    void humanDocsDoNotDuplicateEndpointCatalogs() throws IOException {
        final var root = projectRoot();

        assertFalse(Files.exists(root.resolve("docs/api.md")), "docs/api.md must not exist");
        for (final var doc :
                List.of(
                        root.resolve("README.md"),
                        root.resolve("docs/security.md"),
                        root.resolve("docs/testing.md"),
                        root.resolve("docs/trd.md"))) {
            final var content = Files.readString(doc);
            assertFalse(content.contains("Endpoint Catalog"), () -> doc + " duplicates endpoints");
            assertFalse(
                    content.contains("Endpoint Reference"), () -> doc + " duplicates endpoints");
            assertFalse(content.contains("## API Endpoints"), () -> doc + " duplicates endpoints");
            assertFalse(ENDPOINT_TABLE.matcher(content).find(), () -> doc + " has endpoint table");
        }
    }

    private String generatedOpenApi() throws Exception {
        return mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> openApiPaths(final String openApi) {
        return JsonPath.read(openApi, "$.paths");
    }

    @SuppressWarnings("unchecked")
    private static void assertOperation(
            final Map<String, Object> paths, final String path, final String method) {
        assertTrue(paths.containsKey(path), () -> "Missing OpenAPI path: " + path);
        final var operations = (Map<String, Object>) paths.get(path);
        assertTrue(
                operations.containsKey(method),
                () -> "Missing OpenAPI operation: " + method.toUpperCase() + " " + path);
    }

    private static void assertNoPathContaining(
            final Map<String, Object> paths, final String fragment) {
        assertTrue(
                paths.keySet().stream().noneMatch(path -> path.contains(fragment)),
                () -> "Unsupported OpenAPI path contains: " + fragment);
    }

    private static void assertSchemaHasProperties(
            final String openApi, final String schemaName, final String... propertyNames) {
        final var properties = schemaProperties(openApi, schemaName);
        for (final var propertyName : propertyNames) {
            assertTrue(
                    properties.containsKey(propertyName),
                    () -> schemaName + " is missing property " + propertyName);
        }
    }

    private static void assertSchemaOmitsProperties(
            final String openApi, final String schemaName, final String... propertyNames) {
        final var properties = schemaProperties(openApi, schemaName);
        for (final var propertyName : propertyNames) {
            assertFalse(
                    properties.containsKey(propertyName),
                    () -> schemaName + " exposes property " + propertyName);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> schemaProperties(
            final String openApi, final String schemaName) {
        final Map<String, Object> schemas = JsonPath.read(openApi, "$.components.schemas");
        final var schemaEntry =
                schemas.entrySet().stream()
                        .filter(
                                entry ->
                                        entry.getKey().equals(schemaName)
                                                || entry.getKey().endsWith("." + schemaName))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Missing schema: " + schemaName));
        final var schema = (Map<String, Object>) schemaEntry.getValue();
        final var properties = schema.get("properties");
        assertTrue(properties instanceof Map, () -> schemaName + " has no properties");
        return (Map<String, Object>) properties;
    }

    private static Path projectRoot() {
        final var userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.exists(userDir.resolve("settings.gradle.kts"))) {
            return userDir;
        }
        return userDir.getParent();
    }

    private String regclass(final String qualifiedName) {
        return jdbcTemplate.queryForObject(
                "SELECT to_regclass(?)::text", String.class, qualifiedName);
    }

    private UUID insertAccount(final String email, final String name, final String status) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO platform.account
                    (id, email, name, hashed_password, status, created_at)
                VALUES (?, ?, ?, '$argon2id$test', ?, ?)
                """,
                id,
                email,
                name,
                status,
                Timestamp.from(NOW));
        return id;
    }

    private UUID insertBusiness(final String name, final String slug, final String status) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO platform.business
                    (id, name, slug, timezone, status, created_at)
                VALUES (?, ?, ?, 'Asia/Seoul', ?, ?)
                """,
                id,
                name,
                slug,
                status,
                Timestamp.from(NOW));
        return id;
    }

    private void insertMembership(
            final UUID accountId, final UUID businessId, final String role, final boolean active) {
        jdbcTemplate.update(
                """
                INSERT INTO platform.business_membership
                    (id, account_id, business_id, role, active, created_at, updated_at, disabled_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                accountId,
                businessId,
                role,
                active,
                Timestamp.from(NOW),
                Timestamp.from(NOW),
                active ? null : Timestamp.from(NOW));
    }

    private void insertSettings(final UUID businessId) {
        jdbcTemplate.update(
                """
                INSERT INTO timeslot.business_booking_settings (
                    business_id, slot_duration_minutes, hold_ttl_minutes,
                    cancellation_window_minutes, max_advance_booking_days, created_at, updated_at
                ) VALUES (?, 30, 10, 60, 30, ?, ?)
                """,
                businessId,
                Timestamp.from(NOW),
                Timestamp.from(NOW));
    }

    private void insertResource(
            final UUID businessId, final UUID resourceId, final String name, final String status) {
        jdbcTemplate.update(
                """
                INSERT INTO timeslot.resource (
                    id, business_id, name, description, status, created_at, updated_at
                ) VALUES (?, ?, ?, null, ?, ?, ?)
                """,
                resourceId,
                businessId,
                name,
                status,
                Timestamp.from(NOW),
                Timestamp.from(NOW));
    }

    private static String bearer(final UUID accountId) throws JOSEException {
        return "Bearer " + signedToken(accountId);
    }

    private static String signedToken(final UUID accountId) throws JOSEException {
        final var claims =
                new JWTClaimsSet.Builder()
                        .issuer(JWT_ISSUER)
                        .subject(accountId.toString())
                        .audience(List.of(JWT_AUDIENCE))
                        .issueTime(Date.from(NOW))
                        .expirationTime(Date.from(Instant.now().plusSeconds(86_400)))
                        .jwtID(UUID.randomUUID().toString())
                        .claim("accountId", accountId.toString())
                        .build();

        final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(JWT_SECRET));
        return signedJwt.serialize();
    }

    private static String settingsJson() {
        return """
               {
                 "slotDurationMinutes": 30,
                 "holdTtlMinutes": 10,
                 "cancellationWindowMinutes": 60,
                 "maxAdvanceBookingDays": 30
               }
               """;
    }
}
