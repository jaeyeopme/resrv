package io.resrv.platform.application.account.in;

public record RegisterAccountCommand(String email, String name, String password) {

    public RegisterAccountCommand {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Account password must not be blank");
        }
    }
}
