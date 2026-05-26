package io.resrv.platform.adapter.out.persistence.account;

import io.resrv.platform.domain.account.AccountSignInProtection;
import io.resrv.shared.kernel.AccountId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "platform", name = "account_sign_in_protection")
class AccountSignInProtectionJpaEntity {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "failed_password_attempts", nullable = false)
    private int failedPasswordAttempts;

    @Column(name = "password_reset_required", nullable = false)
    private boolean passwordResetRequired;

    @Column(name = "password_reset_required_at")
    private Instant passwordResetRequiredAt;

    @Column(name = "last_failed_at")
    private Instant lastFailedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountSignInProtectionJpaEntity() {}

    private AccountSignInProtectionJpaEntity(
            final UUID accountId,
            final int failedPasswordAttempts,
            final boolean passwordResetRequired,
            final Instant passwordResetRequiredAt,
            final Instant lastFailedAt,
            final Instant updatedAt) {
        this.accountId = accountId;
        this.failedPasswordAttempts = failedPasswordAttempts;
        this.passwordResetRequired = passwordResetRequired;
        this.passwordResetRequiredAt = passwordResetRequiredAt;
        this.lastFailedAt = lastFailedAt;
        this.updatedAt = updatedAt;
    }

    static AccountSignInProtectionJpaEntity fromDomain(final AccountSignInProtection protection) {
        return new AccountSignInProtectionJpaEntity(
                protection.accountId().value(),
                protection.failedPasswordAttempts(),
                protection.passwordResetRequired(),
                protection.passwordResetRequiredAt(),
                protection.lastFailedAt(),
                protection.updatedAt());
    }

    AccountSignInProtection toDomain() {
        return new AccountSignInProtection(
                AccountId.of(accountId),
                failedPasswordAttempts,
                passwordResetRequired,
                passwordResetRequiredAt,
                lastFailedAt,
                updatedAt);
    }
}
