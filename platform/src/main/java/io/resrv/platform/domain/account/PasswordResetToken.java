package io.resrv.platform.domain.account;

public record PasswordResetToken(String value) {

    public PasswordResetToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password reset token must not be blank");
        }
    }
}
