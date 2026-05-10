package io.resrv.adapter.out.persistence.auth;

import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class RevokedTokenCleanup {

    private final RevokedTokenJpaRepository revokedTokenJpaRepository;
    private final Clock clock;

    RevokedTokenCleanup(
            final RevokedTokenJpaRepository revokedTokenJpaRepository, final Clock clock) {
        this.revokedTokenJpaRepository = revokedTokenJpaRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${resrv.auth.revocation-cleanup-interval:PT1H}")
    @Transactional
    void deleteExpiredRevocations() {
        revokedTokenJpaRepository.deleteByExpiresAtBefore(clock.instant());
    }
}
