package io.resrv.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ReservationMvpIntegrationTest extends AbstractIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000010001");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000010002");
    private static final UUID RESOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000010003");
    private static final String TENANT_SLUG = "reservation-mvp";
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private static final String FIRST_SLOT_START = "2030-01-07T09:00:00Z";
    private static final LocalDate PAST_MONDAY = LocalDate.of(2026, 5, 11);
    private static final String PAST_SLOT_09 = "2026-05-11T09:00:00Z";
    private static final String PAST_SLOT_10 = "2026-05-11T10:00:00Z";
    private static final String PAST_SLOT_11 = "2026-05-11T11:00:00Z";

    @BeforeEach
    void setUpReservationFixture() {
        jdbcTemplate.update(
                """
                INSERT INTO tenant (
                    id,
                    name,
                    slug,
                    timezone,
                    slot_duration,
                    hold_ttl,
                    cancellation_window,
                    status,
                    created_at
                )
                VALUES (?, 'Reservation MVP Salon', ?, 'UTC', 60, 15, 0, 'ACTIVE', NOW())
                """,
                TENANT_ID,
                TENANT_SLUG);
        jdbcTemplate.update(
                """
                INSERT INTO resource (
                    id,
                    tenant_id,
                    slug,
                    name,
                    description,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, 'haircut', 'Haircut Chair', 'Main chair', 'ACTIVE', NOW(), NOW())
                """,
                RESOURCE_ID,
                TENANT_ID);
    }

    @Test
    void customerCanReserveAvailableSlotAndAdminCanAuditResourceSchedule() throws Exception {
        final var adminAuthorization = adminBearer();
        final var customerAuthorization = customerBearer("customer@example.com", "Customer One");

        mockMvc.perform(
                        put(
                                        "/api/resources/{resourceId}/weekly-availability/{dayOfWeek}",
                                        RESOURCE_ID,
                                        1)
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(weeklyAvailabilityJson("09:00", "11:00")))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put(
                                        "/api/resources/{resourceId}/weekly-availability/{dayOfWeek}",
                                        RESOURCE_ID,
                                        1)
                                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(weeklyAvailabilityJson("09:00", "11:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(RESOURCE_ID.toString()))
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.endTime").value("11:00:00"));

        mockMvc.perform(
                        get("/api/resources/{resourceId}/slots", RESOURCE_ID)
                                .param("date", MONDAY.toString())
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startAt").value(FIRST_SLOT_START))
                .andExpect(jsonPath("$[1].startAt").value("2030-01-07T10:00:00Z"));

        final var holdResult =
                mockMvc.perform(
                                post("/api/reservation-holds")
                                        .header(HttpHeaders.AUTHORIZATION, customerAuthorization)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(holdJson(FIRST_SLOT_START)))
                        .andExpect(status().isCreated())
                        .andExpect(
                                header().string(
                                                HttpHeaders.LOCATION,
                                                containsString("/api/me/reservations/")))
                        .andExpect(jsonPath("$.resourceId").value(RESOURCE_ID.toString()))
                        .andExpect(jsonPath("$.startAt").value(FIRST_SLOT_START))
                        .andExpect(jsonPath("$.status").value("HELD"))
                        .andReturn();
        final var reservationId = stringField(holdResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(
                        post("/api/reservation-holds")
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(holdJson(FIRST_SLOT_START)))
                .andExpect(status().isConflict());

        mockMvc.perform(
                        post("/api/reservation-holds/{reservationId}/confirm", reservationId)
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(
                        get("/api/me/reservations")
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reservationId))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        mockMvc.perform(
                        post("/api/me/reservations/{reservationId}/cancel", reservationId)
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.status").value("CUSTOMER_CANCELLED"));

        mockMvc.perform(
                        get("/api/resources/{resourceId}/slots", RESOURCE_ID)
                                .param("date", MONDAY.toString())
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startAt").value(FIRST_SLOT_START));

        mockMvc.perform(
                        get("/api/resources/{resourceId}/reservations", RESOURCE_ID)
                                .param("date", MONDAY.toString())
                                .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reservationId))
                .andExpect(jsonPath("$[0].status").value("CUSTOMER_CANCELLED"));
    }

    @Test
    void adminCanSearchAndOperateReservationLifecycle() throws Exception {
        final var adminAuthorization = adminBearer();
        final var customerAuthorization = customerBearer("ops@example.com", "Ops Customer");
        mockMvc.perform(
                        put(
                                        "/api/resources/{resourceId}/weekly-availability/{dayOfWeek}",
                                        RESOURCE_ID,
                                        1)
                                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(weeklyAvailabilityJson("09:00", "12:00")))
                .andExpect(status().isOk());

        final var noShowReservationId = holdAndConfirm(customerAuthorization, PAST_SLOT_09);
        mockMvc.perform(
                        get("/api/reservations")
                                .param("date", PAST_MONDAY.toString())
                                .param("resourceId", RESOURCE_ID.toString())
                                .param("status", "CONFIRMED")
                                .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(noShowReservationId));

        mockMvc.perform(
                        post("/api/reservations/{reservationId}/no-show", noShowReservationId)
                                .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        mockMvc.perform(
                        post("/api/reservation-holds")
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(holdJson(PAST_SLOT_09)))
                .andExpect(status().isCreated());

        final var checkedInReservationId = holdAndConfirm(customerAuthorization, PAST_SLOT_10);
        mockMvc.perform(
                        post("/api/reservations/{reservationId}/check-in", checkedInReservationId)
                                .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
        mockMvc.perform(
                        post("/api/reservation-holds")
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(holdJson(PAST_SLOT_10)))
                .andExpect(status().isConflict());

        final var cancelledReservationId = holdAndConfirm(customerAuthorization, PAST_SLOT_11);
        mockMvc.perform(
                        post(
                                        "/api/reservations/{reservationId}/admin-cancel",
                                        cancelledReservationId)
                                .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMIN_CANCELLED"));
        mockMvc.perform(
                        get("/api/resources/{resourceId}/reservations", RESOURCE_ID)
                                .param("date", PAST_MONDAY.toString())
                                .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    private String holdAndConfirm(final String customerAuthorization, final String slotStart)
            throws Exception {
        final var holdResult =
                mockMvc.perform(
                                post("/api/reservation-holds")
                                        .header(HttpHeaders.AUTHORIZATION, customerAuthorization)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(holdJson(slotStart)))
                        .andExpect(status().isCreated())
                        .andReturn();
        final var reservationId = stringField(holdResult.getResponse().getContentAsString(), "id");
        mockMvc.perform(
                        post("/api/reservation-holds/{reservationId}/confirm", reservationId)
                                .header(HttpHeaders.AUTHORIZATION, customerAuthorization))
                .andExpect(status().isOk());
        return reservationId;
    }

    private String adminBearer() throws Exception {
        final var now = Instant.now();
        return "Bearer " + mintJwt(ADMIN_ID, TENANT_ID, "OWNER", now, now.plusSeconds(1800));
    }

    private String customerBearer(final String email, final String name) throws Exception {
        mockMvc.perform(
                        post("/public/{tenantSlug}/customers", TENANT_SLUG)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(customerRegistrationJson(email, name, "password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value(name));

        final var loginResult =
                mockMvc.perform(
                                post("/public/{tenantSlug}/customers/login", TENANT_SLUG)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(customerLoginJson(email, "password123")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.tokenType").value("Bearer"))
                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                        .andReturn();
        return "Bearer "
                + stringField(loginResult.getResponse().getContentAsString(), "accessToken");
    }

    private static String weeklyAvailabilityJson(final String startTime, final String endTime) {
        return """
                {"startTime": "%s", "endTime": "%s"}
                """
                .formatted(startTime, endTime);
    }

    private static String holdJson(final String startAt) {
        return """
                {"resourceId": "%s", "startAt": "%s"}
                """
                .formatted(RESOURCE_ID, startAt);
    }

    private static String customerRegistrationJson(
            final String email, final String name, final String password) {
        return """
                {"email": "%s", "name": "%s", "password": "%s"}
                """
                .formatted(email, name, password);
    }

    private static String customerLoginJson(final String email, final String password) {
        return """
                {"email": "%s", "password": "%s"}
                """
                .formatted(email, password);
    }

    private static String stringField(final String json, final String field) {
        final var matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
