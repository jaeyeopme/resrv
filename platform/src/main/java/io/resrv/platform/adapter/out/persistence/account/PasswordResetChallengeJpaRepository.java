package io.resrv.platform.adapter.out.persistence.account;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface PasswordResetChallengeJpaRepository
        extends CrudRepository<PasswordResetChallengeJpaEntity, UUID> {

    List<PasswordResetChallengeJpaEntity> findAllByAccountIdAndUsedAtIsNullAndReplacedAtIsNull(
            UUID accountId);

    Optional<PasswordResetChallengeJpaEntity>
            findByTokenDigestAndUsedAtIsNullAndReplacedAtIsNullAndExpiresAtAfter(
                    String tokenDigest, Instant now);
}
