package io.resrv.platform.adapter.out.persistence.account;

import io.resrv.platform.domain.account.SignInAttemptOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "platform", name = "sign_in_attempt")
class SignInAttemptJpaEntity {

    @Id private UUID id;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "email_hash", nullable = false, length = 128)
    private String emailHash;

    @Column(name = "caller_fingerprint", nullable = false)
    private String callerFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private SignInAttemptOutcome outcome;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected SignInAttemptJpaEntity() {}

    SignInAttemptJpaEntity(
            final UUID id,
            final UUID accountId,
            final String emailHash,
            final String callerFingerprint,
            final SignInAttemptOutcome outcome,
            final Instant occurredAt) {
        this.id = id;
        this.accountId = accountId;
        this.emailHash = emailHash;
        this.callerFingerprint = callerFingerprint;
        this.outcome = outcome;
        this.occurredAt = occurredAt;
    }
}
