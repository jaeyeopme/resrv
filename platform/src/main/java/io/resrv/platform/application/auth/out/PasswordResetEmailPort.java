package io.resrv.platform.application.auth.out;

import io.resrv.platform.domain.account.AccountEmail;
import java.time.Instant;

public interface PasswordResetEmailPort {

    void sendPasswordResetEmail(AccountEmail recipient, String resetLink, Instant expiresAt);
}
