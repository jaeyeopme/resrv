package io.resrv.platform.domain.account;

public final class AccountEmailAlreadyExistsException extends RuntimeException {

    public AccountEmailAlreadyExistsException(final AccountEmail email) {
        super("Account email already exists: " + email.value());
    }
}
