package io.resrv.adapter.out.persistence.auth;

import io.resrv.application.auth.out.TokenRevocationPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class TokenRevocationPersistenceAdapter implements TokenRevocationPort {

    private final RevokedTokenJpaRepository revokedTokenJpaRepository;
    private final Clock clock;

    TokenRevocationPersistenceAdapter(
            final RevokedTokenJpaRepository revokedTokenJpaRepository, final Clock clock) {
        this.revokedTokenJpaRepository = revokedTokenJpaRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void revoke(final String jti, final Instant expiration) {
        revokedTokenJpaRepository.upsert(jti, expiration, clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(final String jti) {
        return revokedTokenJpaRepository.existsByJtiAndExpiresAtAfter(jti, clock.instant());
    }
}
