package io.resrv.platform.adapter.out.persistence.account;

import io.resrv.platform.domain.account.PasswordResetChallenge;
import io.resrv.platform.domain.account.PasswordResetChallengeReason;
import io.resrv.shared.kernel.AccountId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "platform", name = "password_reset_challenge")
class PasswordResetChallengeJpaEntity {

    @Id private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "token_digest", nullable = false, length = 128)
    private String tokenDigest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private PasswordResetChallengeReason reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "replaced_at")
    private Instant replacedAt;

    protected PasswordResetChallengeJpaEntity() {}

    private PasswordResetChallengeJpaEntity(
            final UUID id,
            final UUID accountId,
            final String tokenDigest,
            final PasswordResetChallengeReason reason,
            final Instant createdAt,
            final Instant expiresAt,
            final Instant usedAt,
            final Instant replacedAt) {
        this.id = id;
        this.accountId = accountId;
        this.tokenDigest = tokenDigest;
        this.reason = reason;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.replacedAt = replacedAt;
    }

    static PasswordResetChallengeJpaEntity fromDomain(final PasswordResetChallenge challenge) {
        return new PasswordResetChallengeJpaEntity(
                challenge.id(),
                challenge.accountId().value(),
                challenge.tokenDigest(),
                challenge.reason(),
                challenge.createdAt(),
                challenge.expiresAt(),
                challenge.usedAt(),
                challenge.replacedAt());
    }

    PasswordResetChallenge toDomain() {
        return new PasswordResetChallenge(
                id,
                AccountId.of(accountId),
                tokenDigest,
                reason,
                createdAt,
                expiresAt,
                usedAt,
                replacedAt);
    }
}
