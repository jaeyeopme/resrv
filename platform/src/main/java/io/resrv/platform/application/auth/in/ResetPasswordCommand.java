package io.resrv.platform.application.auth.in;

public record ResetPasswordCommand(String token, String newPassword) {

    public ResetPasswordCommand {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Password reset token is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
    }
}
