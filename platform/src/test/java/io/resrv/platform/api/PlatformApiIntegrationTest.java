package io.resrv.platform.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
final class PlatformApiIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901";
    private static final String JWT_ISSUER = "resrv-test";
    private static final String JWT_AUDIENCE = "resrv-api";
    private static final Instant TOKEN_NOW = Instant.now();

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakePasswordResetEmailAdapter.FakePasswordResetEmailPort fakePasswordResetEmailPort;

    @BeforeEach
    void setUp() {
        fakePasswordResetEmailPort.clear();
        jdbcTemplate.update("DELETE FROM platform.sign_in_attempt");
        jdbcTemplate.update("DELETE FROM platform.password_reset_challenge");
        jdbcTemplate.update("DELETE FROM platform.account_sign_in_protection");
        jdbcTemplate.update("DELETE FROM platform.business_membership_audit_entry");
        jdbcTemplate.update("DELETE FROM platform.business_membership");
        jdbcTemplate.update("DELETE FROM platform.business");
        jdbcTemplate.update("DELETE FROM platform.account");
    }

    @Test
    void accountCanRegisterLoginAndCreateBusiness() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "owner@example.com",
                                          "name": "Owner One",
                                          "password": "passw0rd!"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.name").value("Owner One"));

        final var loginResponse =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "email": "owner@example.com",
                                                  "password": "passw0rd!"
                                                }
                                                """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken", notNullValue()))
                        .andExpect(jsonPath("$.expiresIn").value(3600))
                        .andExpect(jsonPath("$.tokenType").doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final String accessToken = JsonPath.read(loginResponse, "$.accessToken");

        mockMvc.perform(
                        post("/api/businesses")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Salon A",
                                          "slug": "salon-a",
                                          "timezone": "Asia/Seoul"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Salon A"))
                .andExpect(jsonPath("$.slug").value("salon-a"))
                .andExpect(jsonPath("$.timezone").value("Asia/Seoul"));
    }

    @Test
    void nullLoginJsonBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordResetEmailUnblocksAccountAfterRepeatedFailuresWithoutEnumeration()
            throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "owner@example.com",
                                          "name": "Owner One",
                                          "password": "passw0rd!"
                                        }
                                        """))
                .andExpect(status().isCreated());

        for (var attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {
                                              "email": "owner@example.com",
                                              "password": "wrong-password"
                                            }
                                            """))
                    .andExpect(status().isUnauthorized());
        }

        final var deliveries = fakePasswordResetEmailPort.deliveries();
        assertEquals(1, deliveries.size());
        assertEquals("owner@example.com", deliveries.getFirst().recipient());
        assertTrue(
                deliveries
                        .getFirst()
                        .resetLink()
                        .startsWith("https://app.example.com/reset-password?token="));
        final var resetToken = tokenFromResetLink(deliveries.getFirst().resetLink());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "owner@example.com",
                                          "password": "passw0rd!"
                                        }
                                        """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/auth/password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "token": "%s",
                                          "newPassword": "new-passw0rd!"
                                        }
                                        """
                                                .formatted(resetToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reset").value(true));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "owner@example.com",
                                          "password": "passw0rd!"
                                        }
                                        """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "owner@example.com",
                                          "password": "new-passw0rd!"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "missing@example.com",
                                          "password": "wrong-password"
                                        }
                                        """))
                .andExpect(status().isUnauthorized());
        assertEquals(1, fakePasswordResetEmailPort.deliveries().size());
    }

    @Test
    void generatedOpenApiDocumentsAccountAuthAndBusinessResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paths['/api/accounts'].post.summary").value("Register account"))
                .andExpect(
                        jsonPath("$.paths['/api/accounts'].post.responses['201'].description")
                                .value("Account registered"))
                .andExpect(
                        jsonPath("$.paths['/api/accounts'].post.responses['400'].description")
                                .value("Validation failure"))
                .andExpect(
                        jsonPath("$.paths['/api/accounts'].post.responses['409'].description")
                                .value("Email already registered"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.summary").value("Sign in"))
                .andExpect(
                        jsonPath("$.paths['/api/auth/login'].post.responses['200'].description")
                                .value("Sign-in succeeded"))
                .andExpect(
                        jsonPath("$.paths['/api/auth/login'].post.responses['400'].description")
                                .value("Malformed request"))
                .andExpect(
                        jsonPath("$.paths['/api/auth/login'].post.responses['401'].description")
                                .value(
                                        "Sign-in failed or password reset is required without account enumeration"))
                .andExpect(
                        jsonPath("$.paths['/api/auth/password-reset'].post.summary")
                                .value("Reset password"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/auth/password-reset'].post.responses['200'].description")
                                .value("Password reset succeeded"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/auth/password-reset'].post.responses['400'].description")
                                .value("Reset token, password, or request body is invalid"))
                .andExpect(
                        jsonPath("$.paths['/api/businesses'].post.summary")
                                .value("Create business"))
                .andExpect(
                        jsonPath("$.paths['/api/businesses'].post.responses['201'].description")
                                .value("Business created"))
                .andExpect(
                        jsonPath("$.paths['/api/businesses'].post.responses['400'].description")
                                .value("Validation failure"))
                .andExpect(
                        jsonPath("$.paths['/api/businesses'].post.responses['401'].description")
                                .value("Authentication is required"))
                .andExpect(
                        jsonPath("$.paths['/api/businesses'].post.responses['403'].description")
                                .value("Active account is required"))
                .andExpect(
                        jsonPath("$.paths['/api/businesses'].post.responses['409'].description")
                                .value("Business slug already exists"));
    }

    private static String tokenFromResetLink(final String resetLink) {
        return resetLink.substring(resetLink.indexOf("token=") + "token=".length());
    }

    @Test
    void tokenWithoutJtiIsRejectedBeforeController() throws Exception {
        final var accountId = UUID.randomUUID().toString();
        assertBusinessTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        TOKEN_NOW.plusSeconds(3600),
                        accountId,
                        accountId,
                        null));
    }

    @Test
    void tokenWithWrongAudienceIsRejectedBeforeController() throws Exception {
        final var accountId = UUID.randomUUID().toString();
        assertBusinessTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        "wrong-audience",
                        TOKEN_NOW,
                        TOKEN_NOW.plusSeconds(3600),
                        accountId,
                        accountId,
                        UUID.randomUUID().toString()));
    }

    @Test
    void tokenWithWrongIssuerIsRejectedBeforeController() throws Exception {
        final var accountId = UUID.randomUUID().toString();
        assertBusinessTokenRejected(
                signedToken(
                        "wrong-issuer",
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        TOKEN_NOW.plusSeconds(3600),
                        accountId,
                        accountId,
                        UUID.randomUUID().toString()));
    }

    @Test
    void expiredTokenIsRejectedBeforeController() throws Exception {
        final var accountId = UUID.randomUUID().toString();
        assertBusinessTokenRejected(
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
        assertBusinessTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        TOKEN_NOW.plusSeconds(3600),
                        UUID.randomUUID().toString(),
                        "not-a-uuid",
                        UUID.randomUUID().toString()));
    }

    @Test
    void tokenWithMalformedSubjectIsRejectedBeforeController() throws Exception {
        assertBusinessTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        TOKEN_NOW.plusSeconds(3600),
                        "not-a-uuid",
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString()));
    }

    @Test
    void tokenWithMismatchedSubjectAndAccountIdIsRejectedBeforeController() throws Exception {
        assertBusinessTokenRejected(
                signedToken(
                        JWT_ISSUER,
                        JWT_AUDIENCE,
                        TOKEN_NOW,
                        TOKEN_NOW.plusSeconds(3600),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString()));
    }

    @Test
    void missingBearerTokenIsRejected() throws Exception {
        mockMvc.perform(
                        post("/api/businesses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(businessRequestJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generatedDocumentationEndpointsRemainPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }

    @Test
    void inactiveAccountTokenIsDeniedOnProtectedAction() throws Exception {
        final var accountId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        jdbcTemplate.update(
                """
                INSERT INTO platform.account (
                    id, email, name, hashed_password, status, created_at
                ) VALUES (?, 'disabled@example.com', 'Disabled', '$argon2id$test', 'DISABLED', ?)
                """,
                accountId,
                Timestamp.from(Instant.now()));

        mockMvc.perform(
                        post("/api/businesses")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + signedToken(accountId.toString()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(businessRequestJson()))
                .andExpect(status().isForbidden());
    }

    private void assertBusinessTokenRejected(final String accessToken) throws Exception {
        mockMvc.perform(
                        post("/api/businesses")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(businessRequestJson()))
                .andExpect(status().isUnauthorized());
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
        final var builder =
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .subject(subject)
                        .audience(List.of(audience))
                        .issueTime(Date.from(issuedAt))
                        .expirationTime(Date.from(expiresAt))
                        .claim("accountId", accountId);
        if (jwtId != null) {
            builder.jwtID(jwtId);
        }

        final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
        signedJwt.sign(new MACSigner(JWT_SECRET));
        return signedJwt.serialize();
    }

    private static String signedToken(final String accountId) throws JOSEException {
        return signedToken(
                JWT_ISSUER,
                JWT_AUDIENCE,
                TOKEN_NOW,
                TOKEN_NOW.plusSeconds(3600),
                accountId,
                accountId,
                UUID.randomUUID().toString());
    }

    private static String businessRequestJson() {
        return """
                {
                  "name": "Rejected Business",
                  "slug": "rejected-business",
                  "timezone": "Asia/Seoul"
                }
                """;
    }
}
