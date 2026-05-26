package io.resrv.platform.adapter.out.persistence.account;

import io.resrv.platform.application.auth.out.SignInAttemptCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionQueryPort;
import io.resrv.platform.domain.account.AccountSignInProtection;
import io.resrv.platform.domain.account.PasswordResetChallenge;
import io.resrv.platform.domain.account.SignInAttemptOutcome;
import io.resrv.shared.kernel.AccountId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class AccountSecurityPersistenceAdapter
        implements SignInAttemptCommandPort,
                SignInProtectionCommandPort,
                SignInProtectionQueryPort {

    private final SignInAttemptJpaRepository signInAttemptJpaRepository;
    private final AccountSignInProtectionJpaRepository protectionJpaRepository;
    private final PasswordResetChallengeJpaRepository challengeJpaRepository;
    private final EntityManager entityManager;

    AccountSecurityPersistenceAdapter(
            final SignInAttemptJpaRepository signInAttemptJpaRepository,
            final AccountSignInProtectionJpaRepository protectionJpaRepository,
            final PasswordResetChallengeJpaRepository challengeJpaRepository,
            final EntityManager entityManager) {
        this.signInAttemptJpaRepository = signInAttemptJpaRepository;
        this.protectionJpaRepository = protectionJpaRepository;
        this.challengeJpaRepository = challengeJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void recordAttempt(
            final Optional<AccountId> accountId,
            final String emailHash,
            final String callerFingerprint,
            final SignInAttemptOutcome outcome,
            final Instant occurredAt) {
        signInAttemptJpaRepository.save(
                new SignInAttemptJpaEntity(
                        UUID.randomUUID(),
                        accountId.map(AccountId::value).orElse(null),
                        emailHash,
                        callerFingerprint,
                        outcome,
                        occurredAt));
        entityManager.flush();
    }

    @Override
    public AccountSignInProtection recordFailedPasswordAttempt(
            final AccountId accountId, final Instant occurredAt) {
        final var protection =
                protectionJpaRepository
                        .findById(accountId.value())
                        .map(AccountSignInProtectionJpaEntity::toDomain)
                        .map(existing -> existing.incrementFailedAttempt(occurredAt))
                        .orElseGet(
                                () ->
                                        AccountSignInProtection.failedAttempt(
                                                accountId, 1, occurredAt));
        protectionJpaRepository.save(AccountSignInProtectionJpaEntity.fromDomain(protection));
        entityManager.flush();
        return protection;
    }

    @Override
    public void clearProtection(final AccountId accountId, final Instant updatedAt) {
        protectionJpaRepository.save(
                AccountSignInProtectionJpaEntity.fromDomain(
                        new AccountSignInProtection(accountId, 0, false, null, null, updatedAt)));
        entityManager.flush();
    }

    @Override
    public PasswordResetChallenge createPasswordResetChallenge(
            final PasswordResetChallenge challenge) {
        final var saved =
                challengeJpaRepository
                        .save(PasswordResetChallengeJpaEntity.fromDomain(challenge))
                        .toDomain();
        entityManager.flush();
        return saved;
    }

    @Override
    public void replaceActivePasswordResetChallenges(
            final AccountId accountId, final Instant replacedAt) {
        final var replaced =
                challengeJpaRepository
                        .findAllByAccountIdAndUsedAtIsNullAndReplacedAtIsNull(accountId.value())
                        .stream()
                        .map(PasswordResetChallengeJpaEntity::toDomain)
                        .map(
                                challenge ->
                                        new PasswordResetChallenge(
                                                challenge.id(),
                                                challenge.accountId(),
                                                challenge.tokenDigest(),
                                                challenge.reason(),
                                                challenge.createdAt(),
                                                challenge.expiresAt(),
                                                challenge.usedAt(),
                                                replacedAt))
                        .map(PasswordResetChallengeJpaEntity::fromDomain)
                        .toList();
        challengeJpaRepository.saveAll(replaced);
        entityManager.flush();
    }

    @Override
    public void markPasswordResetChallengeUsed(
            final PasswordResetChallenge challenge, final Instant usedAt) {
        challengeJpaRepository.save(
                PasswordResetChallengeJpaEntity.fromDomain(
                        new PasswordResetChallenge(
                                challenge.id(),
                                challenge.accountId(),
                                challenge.tokenDigest(),
                                challenge.reason(),
                                challenge.createdAt(),
                                challenge.expiresAt(),
                                usedAt,
                                challenge.replacedAt())));
        entityManager.flush();
    }

    @Override
    public Optional<AccountSignInProtection> findProtection(final AccountId accountId) {
        return protectionJpaRepository
                .findById(accountId.value())
                .map(AccountSignInProtectionJpaEntity::toDomain);
    }

    @Override
    public boolean requiresPasswordReset(final AccountId accountId) {
        return findProtection(accountId)
                .map(AccountSignInProtection::passwordResetRequired)
                .orElse(false);
    }

    @Override
    public Optional<PasswordResetChallenge> findActivePasswordResetChallengeByDigest(
            final String tokenDigest, final Instant now) {
        return challengeJpaRepository
                .findByTokenDigestAndUsedAtIsNullAndReplacedAtIsNullAndExpiresAtAfter(
                        tokenDigest, now)
                .map(PasswordResetChallengeJpaEntity::toDomain);
    }
}
