package io.resrv.platform.domain.account;

import io.resrv.shared.kernel.AccountId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PasswordResetChallenge(
        UUID id,
        AccountId accountId,
        String tokenDigest,
        PasswordResetChallengeReason reason,
        Instant createdAt,
        Instant expiresAt,
        Instant usedAt,
        Instant replacedAt) {

    public PasswordResetChallenge {
        Objects.requireNonNull(id, "Challenge id must not be null");
        Objects.requireNonNull(accountId, "Account id must not be null");
        Objects.requireNonNull(tokenDigest, "Token digest must not be null");
        if (tokenDigest.isBlank()) {
            throw new IllegalArgumentException("Token digest must not be blank");
        }
        Objects.requireNonNull(reason, "Challenge reason must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(expiresAt, "Expires at must not be null");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Expires at must be after created at");
        }
    }

    public static PasswordResetChallenge create(
            final AccountId accountId,
            final String tokenDigest,
            final Instant createdAt,
            final Instant expiresAt) {
        return new PasswordResetChallenge(
                UUID.randomUUID(),
                accountId,
                tokenDigest,
                PasswordResetChallengeReason.FAILED_PASSWORD_ATTEMPTS,
                createdAt,
                expiresAt,
                null,
                null);
    }

    public boolean activeAt(final Instant now) {
        return usedAt == null && replacedAt == null && expiresAt.isAfter(now);
    }
}
