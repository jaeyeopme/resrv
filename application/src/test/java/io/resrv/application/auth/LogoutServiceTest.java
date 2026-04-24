package io.resrv.application.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.resrv.application.auth.in.LogoutCommand;
import io.resrv.application.auth.out.TokenRevocationPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogoutServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2025-01-01T00:00:00Z");

    private TokenRevocationPort tokenRevocationPort;
    private LogoutService service;

    @BeforeEach
    void setUp() {
        tokenRevocationPort = mock(TokenRevocationPort.class);
        final var clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new LogoutService(clock, tokenRevocationPort);
    }

    @Test
    void logout_validToken() {
        final var expiration = FIXED_NOW.plusSeconds(3600);
        final var command = new LogoutCommand("jti-abc-123", expiration);

        service.logout(command);

        verify(tokenRevocationPort).revoke("jti-abc-123", expiration);
    }

    @Test
    void logout_expiredToken() {
        final var expiration = FIXED_NOW.minusSeconds(3600);
        final var command = new LogoutCommand("jti-abc-123", expiration);

        service.logout(command);

        verifyNoInteractions(tokenRevocationPort);
    }

    @Test
    void logout_exactlyExpired() {
        final var command = new LogoutCommand("jti-abc-123", FIXED_NOW);

        service.logout(command);

        verifyNoInteractions(tokenRevocationPort);
    }
}
