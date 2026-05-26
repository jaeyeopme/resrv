package io.resrv.platform.application.auth;

public class PasswordResetRequiredException extends RuntimeException {

    public PasswordResetRequiredException() {
        super("Password reset is required");
    }
}
