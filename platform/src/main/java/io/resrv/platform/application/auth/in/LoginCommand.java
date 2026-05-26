package io.resrv.platform.application.auth.in;

public record LoginCommand(String email, String password, String callerFingerprint) {

    public LoginCommand(final String email, final String password) {
        this(email, password, "unknown");
    }
}
