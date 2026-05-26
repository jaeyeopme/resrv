package io.resrv.platform.domain.account;

import io.resrv.shared.kernel.AccountId;
import java.time.Instant;
import java.util.Objects;

public record AccountSignInProtection(
        AccountId accountId,
        int failedPasswordAttempts,
        boolean passwordResetRequired,
        Instant passwordResetRequiredAt,
        Instant lastFailedAt,
        Instant updatedAt) {

    public AccountSignInProtection {
        Objects.requireNonNull(accountId, "Account id must not be null");
        if (failedPasswordAttempts < 0) {
            throw new IllegalArgumentException("Failed password attempts must not be negative");
        }
        if (passwordResetRequired && passwordResetRequiredAt == null) {
            throw new IllegalArgumentException("Password reset required at must be set");
        }
        if (!passwordResetRequired && passwordResetRequiredAt != null) {
            throw new IllegalArgumentException("Password reset required at must be empty");
        }
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
    }

    public static AccountSignInProtection failedAttempt(
            final AccountId accountId, final int failedPasswordAttempts, final Instant now) {
        final var requiresReset = failedPasswordAttempts >= 5;
        return new AccountSignInProtection(
                accountId,
                failedPasswordAttempts,
                requiresReset,
                requiresReset ? now : null,
                now,
                now);
    }

    public AccountSignInProtection incrementFailedAttempt(final Instant now) {
        final var nextAttempts = failedPasswordAttempts + 1;
        final var requiresReset = passwordResetRequired || nextAttempts >= 5;
        final var requiredAt =
                requiresReset ? Objects.requireNonNullElse(passwordResetRequiredAt, now) : null;
        return new AccountSignInProtection(
                accountId, nextAttempts, requiresReset, requiredAt, now, now);
    }

    public AccountSignInProtection clear(final Instant now) {
        return new AccountSignInProtection(accountId, 0, false, null, null, now);
    }
}
