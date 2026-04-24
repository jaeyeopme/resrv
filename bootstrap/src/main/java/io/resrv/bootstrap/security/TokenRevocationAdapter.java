package io.resrv.bootstrap.security;

import io.resrv.application.auth.out.TokenRevocationPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class TokenRevocationAdapter implements TokenRevocationPort {

    private final JtiBlacklist blacklist;

    TokenRevocationAdapter(final Clock clock) {
        this.blacklist = new JtiBlacklist(clock);
    }

    @Override
    public void revoke(final String jti, final Instant expiration) {
        blacklist.add(jti, expiration);
    }

    @Override
    public boolean isRevoked(final String jti) {
        return blacklist.contains(jti);
    }
}
