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
    }

    private static String signedToken(final UUID accountId) throws JOSEException {
        final var claims =
                new JWTClaimsSet.Builder()
                        .issuer(JWT_ISSUER)
                        .subject(accountId.toString())
                        .audience(List.of(JWT_AUDIENCE))
                        .issueTime(Date.from(TOKEN_NOW))
                        .expirationTime(Date.from(TOKEN_NOW.plusSeconds(86_400)))
                        .jwtID(UUID.randomUUID().toString())
                        .claim("accountId", accountId.toString())
                        .build();
        final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(JWT_SECRET));
        return signedJwt.serialize();
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
