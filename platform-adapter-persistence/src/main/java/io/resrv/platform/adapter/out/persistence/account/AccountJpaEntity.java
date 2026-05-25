package io.resrv.platform.adapter.out.persistence.account;

import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountName;
import io.resrv.platform.domain.account.AccountStatus;
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
@Table(schema = "platform", name = "account")
class AccountJpaEntity {

    @Id private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountJpaEntity() {}

    AccountJpaEntity(
            final UUID id,
            final String email,
            final String name,
            final String hashedPassword,
            final AccountStatus status,
            final Instant createdAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.hashedPassword = hashedPassword;
        this.status = status;
        this.createdAt = createdAt;
    }

    static AccountJpaEntity fromDomain(final Account account) {
        return new AccountJpaEntity(
                account.id().value(),
                account.email().value(),
                account.name().value(),
                account.hashedPassword(),
                account.status(),
                account.createdAt());
    }

    Account toDomain() {
        return Account.reconstitute(
                AccountId.of(id),
                new AccountEmail(email),
                new AccountName(name),
                hashedPassword,
                status,
                createdAt);
    }
}
