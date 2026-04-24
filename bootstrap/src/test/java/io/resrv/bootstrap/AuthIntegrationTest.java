package io.resrv.bootstrap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.resrv.application.auth.AuthenticationFailedException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String TENANT_SLUG = "auth-test-salon";
    private static final String ADMIN_EMAIL = "admin@auth-test.com";

    private static final String ADMIN_PASSWORD = "correct-password";

    private static final String WRONG_SECRET_KEY =
            "wrong-secret-key-that-is-also-at-least-32-bytes!!!";

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM admin");
        jdbcTemplate.execute("DELETE FROM tenant");

        jdbcTemplate.update(
                """
                INSERT INTO tenant (id, name, slug, timezone, slot_duration, hold_ttl, cancellation_window, created_at)
                VALUES (?, 'Auth Test Salon', ?, 'UTC', 60, 15, 0, NOW())
                """,
                TENANT_ID,
                TENANT_SLUG);

        final var hashedPassword = passwordEncoder.encode(ADMIN_PASSWORD);
        jdbcTemplate.update(
                """
                INSERT INTO admin (id, tenant_id, email, hashed_password, role, active, created_at)
                VALUES (?, ?, ?, ?, 'OWNER', true, NOW())
                """,
                ADMIN_ID,
                TENANT_ID,
                ADMIN_EMAIL,
                hashedPassword);
    }

    private String mintValidJwt() throws Exception {
        final var now = Instant.now();
        return mintJwt(ADMIN_ID, TENANT_ID, "OWNER", now, now.plusSeconds(1800));
    }

    private String mintExpiredJwt() throws Exception {
        final var now = Instant.now();
        return mintJwt(ADMIN_ID, TENANT_ID, "OWNER", now.minusSeconds(7200), now.minusSeconds(60));
    }

    private static String loginJson(final String email, final String password) {
        return """
                {"email": "%s", "password": "%s"}
                """
                .formatted(email, password);
    }

    @Nested
    class Login {

        @Test
        void login_success() throws Exception {
            mockMvc.perform(
                            post("/public/{slug}/auth/login", TENANT_SLUG)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(loginJson(ADMIN_EMAIL, ADMIN_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.expiresIn").isNumber())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        void login_wrongPassword() throws Exception {
            mockMvc.perform(
                            post("/public/{slug}/auth/login", TENANT_SLUG)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(loginJson(ADMIN_EMAIL, "wrong-password")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(AuthenticationFailedException.MESSAGE))
                    .andExpect(
                            jsonPath("$.instance").value("/public/" + TENANT_SLUG + "/auth/login"));
        }

        @Test
        void login_unknownEmail() throws Exception {
            mockMvc.perform(
                            post("/public/{slug}/auth/login", TENANT_SLUG)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(loginJson("unknown@test.com", ADMIN_PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(AuthenticationFailedException.MESSAGE));
        }

        @Test
        void login_unknownTenant() throws Exception {
            mockMvc.perform(
                            post("/public/{slug}/auth/login", "nonexistent")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(loginJson(ADMIN_EMAIL, ADMIN_PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(AuthenticationFailedException.MESSAGE))
                    .andExpect(jsonPath("$.instance").value("/public/nonexistent/auth/login"));
        }

        @Test
        void login_malformedJson() throws Exception {
            mockMvc.perform(
                            post("/public/{slug}/auth/login", TENANT_SLUG)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{invalid json"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(AuthenticationFailedException.MESSAGE));
        }

        @Test
        void login_emptyBody() throws Exception {
            mockMvc.perform(
                            post("/public/{slug}/auth/login", TENANT_SLUG)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(""))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(AuthenticationFailedException.MESSAGE));
        }

        @Test
        void login_emptyJsonObject() throws Exception {
            mockMvc.perform(
                            post("/public/{slug}/auth/login", TENANT_SLUG)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(AuthenticationFailedException.MESSAGE));
        }

        @Test
        void login_nullFields() throws Exception {
            mockMvc.perform(
                            post("/public/{slug}/auth/login", TENANT_SLUG)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"email": null, "password": null}
                                            """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    @Nested
    class AuthMe {

        @Test
        void authMe_withValidToken() throws Exception {
            final var token = mintValidJwt();
            mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(ADMIN_ID.toString()))
                    .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
                    .andExpect(jsonPath("$.role").value("OWNER"));
        }

        @Test
        void authMe_withoutToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        }

        @Test
        void authMe_withBadSignature() throws Exception {
            final var now = Instant.now();
            final var token =
                    mintJwtWithKey(
                            ADMIN_ID,
                            TENANT_ID,
                            "OWNER",
                            now,
                            now.plusSeconds(1800),
                            WRONG_SECRET_KEY);

            mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void authMe_withMissingRequiredClaim() throws Exception {
            final var now = Instant.now();
            final var claims =
                    new JWTClaimsSet.Builder()
                            .issuer(jwtIssuer)
                            .subject(ADMIN_ID.toString())
                            .audience(List.of(jwtAudience))
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(now.plusSeconds(1800)))
                            .jwtID(UUID.randomUUID().toString())
                            .claim("userId", ADMIN_ID.toString())
                            .claim("tenantId", TENANT_ID.toString())
                            .build();
            final var signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signed.sign(new MACSigner(jwtSecretKey));
            final var token = signed.serialize();

            mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Logout {

        @Test
        void logout_success() throws Exception {
            final var token = mintValidJwt();
            mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        void logout_revokedTokenRejected() throws Exception {
            final var token = mintValidJwt();
            mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void logout_withExpiredToken() throws Exception {
            final var token = mintExpiredJwt();
            mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        void logout_withoutToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
        }

        @Test
        void logout_idempotent() throws Exception {
            final var token = mintValidJwt();
            mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
            mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }
    }
}
