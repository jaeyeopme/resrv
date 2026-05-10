package io.resrv.adapter.out.persistence.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TokenRevocationPersistenceAdapter.class,
    TokenRevocationPersistenceAdapterTest.ClockConfig.class
})
class TokenRevocationPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-10T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private TokenRevocationPersistenceAdapter adapter;

    @Autowired private RevokedTokenJpaRepository repository;


    @Test
    void revokedFutureToken_isSharedAcrossAdapterInstances() {
        adapter.revoke("jti-future", NOW.plusSeconds(1800));
        final var anotherInstanceAdapter =
                new TokenRevocationPersistenceAdapter(repository, fixedClock());

        assertTrue(anotherInstanceAdapter.isRevoked("jti-future"));
    }

    @Test
    void expiredRevocation_isIgnored() {
        adapter.revoke("jti-expired", NOW.minusSeconds(1));

        assertFalse(adapter.isRevoked("jti-expired"));
    }


    @Test
    void revokeIsIdempotent() {
        adapter.revoke("jti-repeat", NOW.plusSeconds(1800));
        adapter.revoke("jti-repeat", NOW.plusSeconds(1800));

        assertTrue(adapter.isRevoked("jti-repeat"));
        final var revokedToken = repository.findById("jti-repeat").orElseThrow();
        assertEquals("jti-repeat", revokedToken.getJti());
        assertEquals(NOW.plusSeconds(1800), revokedToken.getExpiresAt());
        assertEquals(NOW, revokedToken.getRevokedAt());
    }

    private static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfig {

        @Bean
        Clock clock() {
            return fixedClock();
        }
    }
}
