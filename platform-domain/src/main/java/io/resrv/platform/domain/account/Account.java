package io.resrv.platform.domain.account;

import io.resrv.shared.kernel.AccountId;
import java.time.Instant;
import java.util.Objects;

public final class Account {

    private final AccountId id;
    private final AccountEmail email;
    private final AccountName name;
    private final String hashedPassword;
    private final AccountStatus status;
    private final Instant createdAt;

    private Account(
            final AccountId id,
            final AccountEmail email,
            final AccountName name,
            final String hashedPassword,
            final AccountStatus status,
            final Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Account id must not be null");
        this.email = Objects.requireNonNull(email, "Account email must not be null");
        this.name = Objects.requireNonNull(name, "Account name must not be null");
        this.hashedPassword =
                Objects.requireNonNull(hashedPassword, "Account hashed password must not be null");
        if (this.hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Account hashed password must not be blank");
        }
        this.status = Objects.requireNonNull(status, "Account status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Account created at must not be null");
    }

    public static Account create(
            final AccountEmail email,
            final AccountName name,
            final String hashedPassword,
            final Instant now) {
        return new Account(
                AccountId.create(), email, name, hashedPassword, AccountStatus.ACTIVE, now);
    }

    public static Account reconstitute(
            final AccountId id,
            final AccountEmail email,
            final AccountName name,
            final String hashedPassword,
            final AccountStatus status,
            final Instant createdAt) {
        return new Account(id, email, name, hashedPassword, status, createdAt);
    }

    public AccountId id() {
        return id;
    }

    public AccountEmail email() {
        return email;
    }

    public AccountName name() {
        return name;
    }

    public String hashedPassword() {
        return hashedPassword;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean active() {
        return status == AccountStatus.ACTIVE;
    }
}
