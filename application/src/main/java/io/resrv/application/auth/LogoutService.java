package io.resrv.application.auth;

import io.resrv.application.auth.in.LogoutCommand;
import io.resrv.application.auth.in.LogoutUseCase;
import io.resrv.application.auth.out.TokenRevocationPort;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
class LogoutService implements LogoutUseCase {

    private final Clock clock;
    private final TokenRevocationPort tokenRevocationPort;

    LogoutService(final Clock clock, final TokenRevocationPort tokenRevocationPort) {
        this.clock = clock;
        this.tokenRevocationPort = tokenRevocationPort;
    }

    @Override
    public void logout(final LogoutCommand command) {
        if (command.expiration().isAfter(clock.instant())) {
            tokenRevocationPort.revoke(command.jti(), command.expiration());
        }
    }
}
