package io.resrv.bootstrap;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static {
        postgres.start();
    }

    @Autowired protected MockMvc mockMvc;

    @Autowired protected JdbcTemplate jdbcTemplate;

    @Value("${resrv.jwt.secret-key}")
    protected String jwtSecretKey;

    @Value("${resrv.jwt.issuer}")
    protected String jwtIssuer;

    @Value("${resrv.jwt.audience}")
    protected String jwtAudience;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM reservation");
        jdbcTemplate.execute("DELETE FROM resource_availability_exception");
        jdbcTemplate.execute("DELETE FROM resource_weekly_availability");
        jdbcTemplate.execute("DELETE FROM customer");
        jdbcTemplate.execute("DELETE FROM resource");
        jdbcTemplate.execute("DELETE FROM admin");
        jdbcTemplate.execute("DELETE FROM tenant");
    }

    protected String mintJwt(
            final UUID userId,
            final UUID tenantId,
            final String role,
            final Instant issuedAt,
            final Instant expiration)
            throws Exception {
        return mintJwtWithKey(userId, tenantId, role, issuedAt, expiration, jwtSecretKey);
    }

    protected String mintJwtWithKey(
            final UUID userId,
            final UUID tenantId,
            final String role,
            final Instant issuedAt,
            final Instant expiration,
            final String secretKey)
            throws Exception {
        final var claims =
                new JWTClaimsSet.Builder()
                        .issuer(jwtIssuer)
                        .subject(userId.toString())
                        .audience(List.of(jwtAudience))
                        .issueTime(Date.from(issuedAt))
                        .expirationTime(Date.from(expiration))
                        .jwtID(UUID.randomUUID().toString())
                        .claim("userId", userId.toString())
                        .claim("tenantId", tenantId.toString())
                        .claim("role", role)
                        .build();
        final var signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signed.sign(new MACSigner(secretKey));
        return signed.serialize();
    }
}
