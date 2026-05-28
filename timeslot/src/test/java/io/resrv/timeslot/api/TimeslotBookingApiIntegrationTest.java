package io.resrv.timeslot.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:tc:postgresql:16:///resrv",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "resrv.jwt.secret-key=01234567890123456789012345678901",
            "resrv.jwt.issuer=resrv-test",
            "resrv.jwt.audience=resrv-api",
            "resrv.jwt.expiration=3600"
        })
@AutoConfigureMockMvc
final class TimeslotBookingApiIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901";
    private static final String JWT_ISSUER = "resrv-test";
    private static final String JWT_AUDIENCE = "resrv-api";
    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ACCOUNT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BUSINESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant TOKEN_NOW = Instant.parse("2026-05-25T00:00:00Z");

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
        jdbcTemplate.update("DELETE FROM platform.business_membership");
        jdbcTemplate.update("DELETE FROM platform.business");
        jdbcTemplate.update("DELETE FROM platform.account");
        jdbcTemplate.update(
                """
                INSERT INTO platform.account (
                    id, email, name, hashed_password, status, created_at
                ) VALUES (?, 'owner@example.com', 'Owner One', '$2a$10$testhash', 'ACTIVE', ?)
                """,
                ACCOUNT_ID,
                Timestamp.from(TOKEN_NOW));
        jdbcTemplate.update(
                """
                INSERT INTO platform.account (
                    id, email, name, hashed_password, status, created_at
                ) VALUES (?, 'other@example.com', 'Other One', '$2a$10$testhash', 'ACTIVE', ?)
                """,
                OTHER_ACCOUNT_ID,
                Timestamp.from(TOKEN_NOW));
        jdbcTemplate.update(
                """
                INSERT INTO platform.business (
                    id, name, slug, timezone, status, created_at
                ) VALUES (?, 'Salon A', 'salon-a', 'Asia/Seoul', 'ACTIVE', ?)
                """,
                BUSINESS_ID,
                Timestamp.from(TOKEN_NOW));
        jdbcTemplate.update(
                """
                INSERT INTO platform.business_membership (
                    id, account_id, business_id, role, active, created_at
                ) VALUES (?, ?, ?, 'OWNER', true, ?)
                """,
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                ACCOUNT_ID,
                BUSINESS_ID,
                Timestamp.from(TOKEN_NOW));
    }

    @Test
    void inactiveBusinessIsDeniedForProtectedBusinessAction() throws Exception {
        jdbcTemplate.update(
                "UPDATE platform.business SET status = 'INACTIVE' WHERE id = ?", BUSINESS_ID);

        mockMvc.perform(
                        put("/api/businesses/{businessId}/booking-settings", BUSINESS_ID)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(ACCOUNT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "slotDurationMinutes": 30,
                                          "holdTtlMinutes": 10,
                                          "cancellationWindowMinutes": 60,
                                          "maxAdvanceBookingDays": 30
                                        }
                                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveMembershipIsDeniedForProtectedBusinessAction() throws Exception {
        jdbcTemplate.update(
                "UPDATE platform.business_membership SET active = false WHERE account_id = ?",
                ACCOUNT_ID);

        mockMvc.perform(
                        put("/api/businesses/{businessId}/booking-settings", BUSINESS_ID)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(ACCOUNT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "slotDurationMinutes": 30,
                                          "holdTtlMinutes": 10,
                                          "cancellationWindowMinutes": 60,
                                          "maxAdvanceBookingDays": 30
                                        }
                                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanConfigureResourceHoldAndConfirmReservation() throws Exception {
        final var token = signedToken(ACCOUNT_ID);

        mockMvc.perform(
                        put("/api/businesses/{businessId}/booking-settings", BUSINESS_ID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "slotDurationMinutes": 30,
                                          "holdTtlMinutes": 10,
                                          "cancellationWindowMinutes": 60,
                                          "maxAdvanceBookingDays": 30
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessId").value(BUSINESS_ID.toString()));

        final var resourceJson =
                mockMvc.perform(
                                post("/api/businesses/{businessId}/resources", BUSINESS_ID)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Room A",
                                                  "slug": "room-a",
                                                  "description": "Window side"
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id", notNullValue()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final String resourceId = JsonPath.read(resourceJson, "$.id");

        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/resources/{resourceId}"
                                                + "/weekly-schedules/{dayOfWeek}",
                                        BUSINESS_ID,
                                        resourceId,
                                        "MONDAY")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "windows": [
                                            {
                                              "startTime": "10:00:00",
                                              "endTime": "11:00:00"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk());

        final var slotsJson =
                mockMvc.perform(
                                get(
                                                "/api/businesses/{businessId}/resources/{resourceId}/slots",
                                                BUSINESS_ID,
                                                resourceId)
                                        .param("date", "2026-05-25"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].slotId", notNullValue()))
                        .andExpect(jsonPath("$[0].startAt").value("2026-05-25T10:00:00+09:00"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final String slotId = JsonPath.read(slotsJson, "$[0].slotId");

        final var holdJson =
                mockMvc.perform(
                                post("/api/businesses/{businessId}/reservations", BUSINESS_ID)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "resourceId": "%s",
                                                  "slotId": "%s"
                                                }
                                                """
                                                        .formatted(resourceId, slotId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.state").value("HELD"))
                        .andExpect(jsonPath("$.startAt").value("2026-05-25T10:00:00+09:00"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final String reservationId = JsonPath.read(holdJson, "$.id");

        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/reservations/{reservationId}/confirm",
                                        BUSINESS_ID,
                                        reservationId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CONFIRMED"));

        mockMvc.perform(
                        get("/api/businesses/{businessId}/reservations", BUSINESS_ID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("date", "2026-05-25")
                                .param("resourceId", resourceId)
                                .param("customerAccountId", ACCOUNT_ID.toString())
                                .param("state", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reservationId))
                .andExpect(jsonPath("$[0].state").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].startAt").value("2026-05-25T10:00:00+09:00"));

        mockMvc.perform(
                        get("/api/businesses/{businessId}/reservations", BUSINESS_ID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("date", "2026-05-25")
                                .param("state", "HELD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(
                        get("/api/businesses/{businessId}/reservations", BUSINESS_ID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        get("/api/businesses/{businessId}/reservations", BUSINESS_ID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("date", "2026-05-25")
                                .param("state", "BOGUS"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        get("/api/businesses/{businessId}/reservations", BUSINESS_ID)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(OTHER_ACCOUNT_ID))
                                .param("date", "2026-05-25"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookingSettingsReplacementRequiresAllFieldsAndAppliesToFuturePolicy() throws Exception {
        final var token = signedToken(ACCOUNT_ID);
        putSettings(token, 30, 10, 60, 30);

        mockMvc.perform(
                        put("/api/businesses/{businessId}/booking-settings", BUSINESS_ID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "slotDurationMinutes": 15,
                                          "holdTtlMinutes": 5,
                                          "cancellationWindowMinutes": 240
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        assertSettings(30, 10, 60, 30);

        final var resourceId = createResource(token, "Room A", "room-a");
        replaceWeeklySchedule(token, resourceId, "MONDAY", "10:00:00", "11:00:00");

        putSettings(token, 15, 5, 240, 30);

        final var slotsJson =
                mockMvc.perform(
                                get(
                                                "/api/businesses/{businessId}/resources/{resourceId}/slots",
                                                BUSINESS_ID,
                                                resourceId)
                                        .param("date", "2026-05-25"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].startAt").value("2026-05-25T10:00:00+09:00"))
                        .andExpect(jsonPath("$[1].startAt").value("2026-05-25T10:15:00+09:00"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final String slotId = JsonPath.read(slotsJson, "$[0].slotId");

        final var holdJson =
                holdReservation(token, resourceId, slotId)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.holdExpiresAt").value("2026-05-25T09:05:00+09:00"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final String reservationId = JsonPath.read(holdJson, "$.id");

        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/reservations/{reservationId}/confirm",
                                        BUSINESS_ID,
                                        reservationId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/reservations/{reservationId}/cancel",
                                        BUSINESS_ID,
                                        reservationId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void resourceLifecycleReplacementAndActivationKeepReservationsStable() throws Exception {
        final var token = signedToken(ACCOUNT_ID);
        putSettings(token, 30, 10, 60, 30);
        final var resourceId = createResource(token, "Room A", "room-a");
        replaceWeeklySchedule(token, resourceId, "MONDAY", "10:00:00", "11:00:00");
        final var slotId = firstSlotId(resourceId, "2026-05-25");
        final var holdJson =
                holdReservation(token, resourceId, slotId)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final String reservationId = JsonPath.read(holdJson, "$.id");

        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/resources/{resourceId}",
                                        BUSINESS_ID,
                                        resourceId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Room A Updated",
                                          "slug": "room-a-updated",
                                          "description": "Updated",
                                          "slotDurationMinutes": 45,
                                          "holdTtlMinutes": 5,
                                          "cancellationWindowMinutes": 120
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resourceId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.slotDurationMinutes").value(45));

        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/resources/{resourceId}/deactivate",
                                        BUSINESS_ID,
                                        resourceId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/businesses/{businessId}/resources", BUSINESS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(
                        get(
                                        "/api/businesses/{businessId}/resources/{resourceId}/slots",
                                        BUSINESS_ID,
                                        resourceId)
                                .param("date", "2026-05-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(
                        get("/api/businesses/{businessId}/reservations", BUSINESS_ID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("date", "2026-05-25")
                                .param("resourceId", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reservationId))
                .andExpect(jsonPath("$[0].state").value("HELD"));

        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/resources/{resourceId}/activate",
                                        BUSINESS_ID,
                                        resourceId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void resourceLifecycleRequiresBusinessAccessAndRejectsDuplicateSlug() throws Exception {
        final var token = signedToken(ACCOUNT_ID);
        putSettings(token, 30, 10, 60, 30);
        final var resourceA = createResource(token, "Room A", "room-a");
        createResource(token, "Room B", "room-b");

        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/resources/{resourceId}",
                                        BUSINESS_ID,
                                        resourceA)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Room A",
                                          "slug": "room-b",
                                          "description": null
                                        }
                                        """))
                .andExpect(status().isConflict());

        final var otherToken = signedToken(OTHER_ACCOUNT_ID);
        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/resources/{resourceId}",
                                        BUSINESS_ID,
                                        resourceA)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Room A",
                                          "slug": "room-a",
                                          "description": null
                                        }
                                        """))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/resources/{resourceId}/deactivate",
                                        BUSINESS_ID,
                                        resourceA)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/resources/{resourceId}/activate",
                                        BUSINESS_ID,
                                        resourceA)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void scheduleReplacementSupportsClosedOverridesAndInactiveResources() throws Exception {
        final var token = signedToken(ACCOUNT_ID);
        putSettings(token, 30, 10, 60, 30);
        final var resourceId = createResource(token, "Room A", "room-a");
        replaceWeeklySchedule(token, resourceId, "MONDAY", "10:00:00", "11:00:00");

        mockMvc.perform(
                        get(
                                        "/api/businesses/{businessId}/resources/{resourceId}/slots",
                                        BUSINESS_ID,
                                        resourceId)
                                .param("date", "2026-05-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startAt").value("2026-05-25T10:00:00+09:00"));

        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/resources/{resourceId}"
                                                + "/date-schedule-overrides/{date}",
                                        BUSINESS_ID,
                                        resourceId,
                                        "2026-05-25")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"windows\": []}"))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                        "/api/businesses/{businessId}/resources/{resourceId}/slots",
                                        BUSINESS_ID,
                                        resourceId)
                                .param("date", "2026-05-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/resources/{resourceId}/deactivate",
                                        BUSINESS_ID,
                                        resourceId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        replaceWeeklySchedule(token, resourceId, "MONDAY", "12:00:00", "13:00:00");
        mockMvc.perform(
                        get(
                                        "/api/businesses/{businessId}/resources/{resourceId}/slots",
                                        BUSINESS_ID,
                                        resourceId)
                                .param("date", "2026-05-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void generatedOpenApiIncludesLifecycleOperations() throws Exception {
        final var resourcePath = "/api/businesses/{businessId}/resources/{resourceId}";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paths['/api/businesses/{businessId}/booking-settings'].put")
                                .exists())
                .andExpect(
                        jsonPath("$.paths['/api/businesses/{businessId}/resources'].post").exists())
                .andExpect(jsonPath("$.paths['" + resourcePath + "'].put").exists())
                .andExpect(jsonPath("$.paths['" + resourcePath + "/activate'].post").exists())
                .andExpect(jsonPath("$.paths['" + resourcePath + "/deactivate'].post").exists())
                .andExpect(
                        jsonPath("$.paths['" + resourcePath + "/weekly-schedules/{dayOfWeek}'].put")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.paths['"
                                                + resourcePath
                                                + "/date-schedule-overrides/{date}'].put")
                                .exists());
    }

    @Test
    void customerReservationListIsOwnerScopedPagedAndTimezoneRendered() throws Exception {
        final var resourceId = UUID.fromString("00000000-0000-0000-0000-000000000030");
        final var laterReservation = UUID.fromString("00000000-0000-0000-0000-000000000041");
        final var earlierReservation = UUID.fromString("00000000-0000-0000-0000-000000000042");
        insertResource(resourceId, "Room A", "room-a", "ACTIVE");
        insertReservation(
                laterReservation,
                ACCOUNT_ID,
                resourceId,
                "2026-05-25T02:00:00Z",
                "2026-05-25T02:30:00Z",
                "2026-05-25T00:01:00Z",
                "2026-05-25T00:00:00Z",
                null);
        insertReservation(
                earlierReservation,
                ACCOUNT_ID,
                resourceId,
                "2026-05-25T01:00:00Z",
                "2026-05-25T01:30:00Z",
                "2026-05-25T00:02:00Z",
                "2026-05-25T00:00:00Z",
                "2026-05-25T00:00:00Z");
        insertReservation(
                UUID.fromString("00000000-0000-0000-0000-000000000043"),
                OTHER_ACCOUNT_ID,
                resourceId,
                "2026-05-25T03:00:00Z",
                "2026-05-25T03:30:00Z",
                "2026-05-25T00:03:00Z",
                "2026-05-25T00:00:00Z",
                null);

        mockMvc.perform(
                        get("/api/me/reservations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(ACCOUNT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].reservationId").value(laterReservation.toString()))
                .andExpect(jsonPath("$.items[0].business.name").value("Salon A"))
                .andExpect(jsonPath("$.items[0].resource.name").value("Room A"))
                .andExpect(jsonPath("$.items[0].startAt").value("2026-05-25T11:00:00+09:00"))
                .andExpect(jsonPath("$.items[0].state").value("HELD"))
                .andExpect(
                        jsonPath("$.items[1].reservationId").value(earlierReservation.toString()));

        mockMvc.perform(
                        get("/api/me/reservations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(OTHER_ACCOUNT_ID))
                                .param("page", "5")
                                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        mockMvc.perform(
                        get("/api/me/reservations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(ACCOUNT_ID))
                                .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerReservationDetailUsesSameNotFoundForMissingAndNotOwned() throws Exception {
        final var resourceId = UUID.fromString("00000000-0000-0000-0000-000000000031");
        final var reservationId = UUID.fromString("00000000-0000-0000-0000-000000000044");
        final var missingId = UUID.fromString("00000000-0000-0000-0000-000000000045");
        insertResource(resourceId, "Room B", "room-b", "INACTIVE");
        insertReservation(
                reservationId,
                ACCOUNT_ID,
                resourceId,
                "2026-05-25T01:00:00Z",
                "2026-05-25T01:30:00Z",
                "2026-05-24T23:00:00Z",
                "2026-05-24T22:00:00Z",
                null);

        mockMvc.perform(
                        get("/api/me/reservations/{reservationId}", reservationId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(ACCOUNT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
                .andExpect(jsonPath("$.resource.active").value(false))
                .andExpect(jsonPath("$.state").value("EXPIRED"))
                .andExpect(jsonPath("$.holdExpiresAt").value("2026-05-25T08:00:00+09:00"));

        final var notOwned =
                mockMvc.perform(
                                get("/api/me/reservations/{reservationId}", reservationId)
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer " + signedToken(OTHER_ACCOUNT_ID)))
                        .andExpect(status().isNotFound())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final var missing =
                mockMvc.perform(
                                get("/api/me/reservations/{reservationId}", missingId)
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer " + signedToken(OTHER_ACCOUNT_ID)))
                        .andExpect(status().isNotFound())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Assertions.assertEquals(
                (Integer) JsonPath.read(notOwned, "$.status"),
                (Integer) JsonPath.read(missing, "$.status"));
        Assertions.assertEquals(
                (String) JsonPath.read(notOwned, "$.detail"),
                (String) JsonPath.read(missing, "$.detail"));
    }

    @Test
    void customerReservationFiltersApplyBeforePagination() throws Exception {
        final var resourceId = UUID.fromString("00000000-0000-0000-0000-000000000032");
        final var expiredId = UUID.fromString("00000000-0000-0000-0000-000000000046");
        final var confirmedId = UUID.fromString("00000000-0000-0000-0000-000000000047");
        insertResource(resourceId, "Room C", "room-c", "ACTIVE");
        insertReservation(
                expiredId,
                ACCOUNT_ID,
                resourceId,
                "2026-05-25T03:00:00Z",
                "2026-05-25T03:30:00Z",
                "2026-05-24T23:00:00Z",
                "2026-05-24T22:00:00Z",
                null);
        insertReservation(
                confirmedId,
                ACCOUNT_ID,
                resourceId,
                "2026-05-25T01:00:00Z",
                "2026-05-25T01:30:00Z",
                "2026-05-25T00:10:00Z",
                "2026-05-25T00:00:00Z",
                "2026-05-25T00:00:00Z");

        mockMvc.perform(
                        get("/api/me/reservations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(ACCOUNT_ID))
                                .param("state", "CONFIRMED")
                                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].reservationId").value(confirmedId.toString()));

        mockMvc.perform(
                        get("/api/me/reservations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(ACCOUNT_ID))
                                .param("upcoming", "true")
                                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].reservationId").value(confirmedId.toString()));

        mockMvc.perform(
                        get("/api/me/reservations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(ACCOUNT_ID))
                                .param("state", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatedOpenApiIncludesCustomerReservationOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/me/reservations'].get").exists())
                .andExpect(
                        jsonPath("$.paths['/api/me/reservations/{reservationId}'].get").exists());
    }

    @Test
    void tokenWithoutJtiIsRejectedBeforeController() throws Exception {
        final var accountId = ACCOUNT_ID.toString();
        assertReservationsTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        validTokenExpiresAt(),
                        accountId,
                        accountId,
                        null));
    }

    @Test
    void tokenWithWrongAudienceIsRejectedBeforeController() throws Exception {
        final var accountId = ACCOUNT_ID.toString();
        assertReservationsTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        "wrong-audience",
                        TOKEN_NOW,
                        validTokenExpiresAt(),
                        accountId,
                        accountId,
                        UUID.randomUUID().toString()));
    }

    @Test
    void tokenWithWrongIssuerIsRejectedBeforeController() throws Exception {
        final var accountId = ACCOUNT_ID.toString();
        assertReservationsTokenRejected(
                signedToken(
                        "wrong-issuer",
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        validTokenExpiresAt(),
                        accountId,
                        accountId,
                        UUID.randomUUID().toString()));
    }

    @Test
    void expiredTokenIsRejectedBeforeController() throws Exception {
        final var accountId = ACCOUNT_ID.toString();
        assertReservationsTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        JWT_AUDIENCE,
                        TOKEN_NOW.minusSeconds(7200),
                        TOKEN_NOW.minusSeconds(3600),
                        accountId,
                        accountId,
                        UUID.randomUUID().toString()));
    }

    @Test
    void tokenWithMalformedAccountIdIsRejectedBeforeController() throws Exception {
        assertReservationsTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        validTokenExpiresAt(),
                        ACCOUNT_ID.toString(),
                        "not-a-uuid",
                        UUID.randomUUID().toString()));
    }

    @Test
    void tokenWithMalformedSubjectIsRejectedBeforeController() throws Exception {
        assertReservationsTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        validTokenExpiresAt(),
                        "not-a-uuid",
                        ACCOUNT_ID.toString(),
                        UUID.randomUUID().toString()));
    }

    @Test
    void tokenWithMismatchedSubjectAndAccountIdIsRejectedBeforeController() throws Exception {
        assertReservationsTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        validTokenExpiresAt(),
                        ACCOUNT_ID.toString(),
                        OTHER_ACCOUNT_ID.toString(),
                        UUID.randomUUID().toString()));
    }

    private void assertReservationsTokenRejected(final String accessToken) throws Exception {
        mockMvc.perform(
                        get("/api/businesses/{businessId}/reservations", BUSINESS_ID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .param("date", "2026-05-25"))
                .andExpect(status().isUnauthorized());
    }

    private void putSettings(
            final String token,
            final int slotDurationMinutes,
            final int holdTtlMinutes,
            final int cancellationWindowMinutes,
            final int maxAdvanceBookingDays)
            throws Exception {
        mockMvc.perform(
                        put("/api/businesses/{businessId}/booking-settings", BUSINESS_ID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "slotDurationMinutes": %d,
                                          "holdTtlMinutes": %d,
                                          "cancellationWindowMinutes": %d,
                                          "maxAdvanceBookingDays": %d
                                        }
                                        """
                                                .formatted(
                                                        slotDurationMinutes,
                                                        holdTtlMinutes,
                                                        cancellationWindowMinutes,
                                                        maxAdvanceBookingDays)))
                .andExpect(status().isOk());
    }

    private void assertSettings(
            final int slotDurationMinutes,
            final int holdTtlMinutes,
            final int cancellationWindowMinutes,
            final int maxAdvanceBookingDays) {
        final var settings =
                jdbcTemplate.queryForMap(
                        "SELECT slot_duration_minutes, hold_ttl_minutes, "
                                + "cancellation_window_minutes, max_advance_booking_days "
                                + "FROM timeslot.business_booking_settings WHERE business_id = ?",
                        BUSINESS_ID);
        Assertions.assertEquals(slotDurationMinutes, settings.get("slot_duration_minutes"));
        Assertions.assertEquals(holdTtlMinutes, settings.get("hold_ttl_minutes"));
        Assertions.assertEquals(
                cancellationWindowMinutes, settings.get("cancellation_window_minutes"));
        Assertions.assertEquals(maxAdvanceBookingDays, settings.get("max_advance_booking_days"));
    }

    private String createResource(final String token, final String name, final String slug)
            throws Exception {
        final var resourceJson =
                mockMvc.perform(
                                post("/api/businesses/{businessId}/resources", BUSINESS_ID)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "%s",
                                                  "slug": "%s",
                                                  "description": "Window side"
                                                }
                                                """
                                                        .formatted(name, slug)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id", notNullValue()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(resourceJson, "$.id");
    }

    private void replaceWeeklySchedule(
            final String token,
            final String resourceId,
            final String dayOfWeek,
            final String startTime,
            final String endTime)
            throws Exception {
        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/resources/{resourceId}"
                                                + "/weekly-schedules/{dayOfWeek}",
                                        BUSINESS_ID,
                                        resourceId,
                                        dayOfWeek)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "windows": [
                                            {
                                              "startTime": "%s",
                                              "endTime": "%s"
                                            }
                                          ]
                                        }
                                        """
                                                .formatted(startTime, endTime)))
                .andExpect(status().isOk());
    }

    private String firstSlotId(final String resourceId, final String date) throws Exception {
        final var slotsJson =
                mockMvc.perform(
                                get(
                                                "/api/businesses/{businessId}/resources/{resourceId}/slots",
                                                BUSINESS_ID,
                                                resourceId)
                                        .param("date", date))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].slotId", notNullValue()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(slotsJson, "$[0].slotId");
    }

    private ResultActions holdReservation(
            final String token, final String resourceId, final String slotId) throws Exception {
        return mockMvc.perform(
                post("/api/businesses/{businessId}/reservations", BUSINESS_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "resourceId": "%s",
                                  "slotId": "%s"
                                }
                                """
                                        .formatted(resourceId, slotId)));
    }

    private void insertResource(
            final UUID resourceId, final String name, final String slug, final String status) {
        jdbcTemplate.update(
                """
                INSERT INTO timeslot.resource (
                    id, business_id, slug, name, description, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, null, ?, ?, ?)
                """,
                resourceId,
                BUSINESS_ID,
                slug,
                name,
                status,
                Timestamp.from(TOKEN_NOW),
                Timestamp.from(TOKEN_NOW));
    }

    private void insertReservation(
            final UUID reservationId,
            final UUID accountId,
            final UUID resourceId,
            final String startAt,
            final String endAt,
            final String holdExpiresAt,
            final String createdAt,
            final String confirmedAt) {
        jdbcTemplate.update(
                """
                INSERT INTO timeslot.reservation (
                    id, business_id, resource_id, customer_account_id, start_at, end_at,
                    hold_expires_at, created_at, updated_at, confirmed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                reservationId,
                BUSINESS_ID,
                resourceId,
                accountId,
                Timestamp.from(Instant.parse(startAt)),
                Timestamp.from(Instant.parse(endAt)),
                Timestamp.from(Instant.parse(holdExpiresAt)),
                Timestamp.from(Instant.parse(createdAt)),
                Timestamp.from(Instant.parse(createdAt)),
                confirmedAt == null ? null : Timestamp.from(Instant.parse(confirmedAt)));
    }

    private static String signedToken(final UUID accountId) throws JOSEException {
        return signedToken(
                JWT_ISSUER,
                JWT_AUDIENCE,
                TOKEN_NOW,
                validTokenExpiresAt(),
                accountId.toString(),
                accountId.toString(),
                UUID.randomUUID().toString());
    }

    private static String signedToken(
            final String issuer,
            final String audience,
            final Instant issuedAt,
            final Instant expiresAt,
            final String subject,
            final String accountId,
            final String jwtId)
            throws JOSEException {
        final var claims =
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .subject(subject)
                        .audience(List.of(audience))
                        .issueTime(Date.from(issuedAt))
                        .expirationTime(Date.from(expiresAt))
                        .claim("accountId", accountId);
        if (jwtId != null) {
            claims.jwtID(jwtId);
        }

        final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
        signedJwt.sign(new MACSigner(JWT_SECRET));
        return signedJwt.serialize();
    }

    private static Instant validTokenExpiresAt() {
        return Instant.now().plusSeconds(86_400);
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(TOKEN_NOW, ZoneOffset.UTC);
        }
    }
}
