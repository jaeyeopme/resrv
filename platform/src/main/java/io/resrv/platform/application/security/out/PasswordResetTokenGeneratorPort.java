package io.resrv.platform.application.security.out;

import io.resrv.platform.domain.account.PasswordResetToken;

public interface PasswordResetTokenGeneratorPort {

    PasswordResetToken generate();
}
