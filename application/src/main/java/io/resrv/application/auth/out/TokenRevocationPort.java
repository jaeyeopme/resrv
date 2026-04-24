package io.resrv.application.auth.out;

import java.time.Instant;

public interface TokenRevocationPort {

    void revoke(final String jti, final Instant expiration);

    boolean isRevoked(final String jti);
}
