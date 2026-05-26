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
