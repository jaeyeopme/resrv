package io.resrv.platform.application.auth.out;

import io.resrv.platform.domain.account.AccountSignInProtection;
import io.resrv.platform.domain.account.PasswordResetChallenge;
import io.resrv.shared.kernel.AccountId;
import java.time.Instant;
import java.util.Optional;

public interface SignInProtectionQueryPort {

    Optional<AccountSignInProtection> findProtection(AccountId accountId);

    boolean requiresPasswordReset(AccountId accountId);

    Optional<PasswordResetChallenge> findActivePasswordResetChallengeByDigest(
            String tokenDigest, Instant now);
}
