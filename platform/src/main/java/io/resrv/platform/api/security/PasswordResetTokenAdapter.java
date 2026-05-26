package io.resrv.platform.api.security;

import io.resrv.platform.application.security.out.PasswordResetTokenGeneratorPort;
import io.resrv.platform.application.security.out.PasswordResetTokenHashingPort;
import io.resrv.platform.domain.account.PasswordResetToken;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
class PasswordResetTokenAdapter
        implements PasswordResetTokenGeneratorPort, PasswordResetTokenHashingPort {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public PasswordResetToken generate() {
        final var bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return new PasswordResetToken(
                Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    @Override
    public String digest(final PasswordResetToken token) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            final var bytes = digest.digest(token.value().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
