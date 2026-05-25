package io.resrv.platform.application.account.in;

import io.resrv.platform.domain.account.Account;
import java.util.UUID;

public record RegisterAccountResult(UUID id, String email, String name) {

    public static RegisterAccountResult from(final Account account) {
        return new RegisterAccountResult(
                account.id().value(), account.email().value(), account.name().value());
    }
}
