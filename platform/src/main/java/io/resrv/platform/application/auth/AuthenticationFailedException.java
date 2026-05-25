package io.resrv.platform.application.auth;

public final class AuthenticationFailedException extends RuntimeException {

    public static final String MESSAGE = "Invalid credentials";

    public AuthenticationFailedException() {
        super(MESSAGE);
    }
}
