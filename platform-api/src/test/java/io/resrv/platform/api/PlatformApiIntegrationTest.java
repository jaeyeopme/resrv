package io.resrv.platform.api;

import static org.hamcrest.Matchers.notNullValue;
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
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
final class PlatformApiIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901";
    private static final String JWT_ISSUER = "resrv-test";
    private static final String JWT_AUDIENCE = "resrv-api";
    private static final Instant TOKEN_NOW = Instant.now();

    @Autowired private MockMvc mockMvc;

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
