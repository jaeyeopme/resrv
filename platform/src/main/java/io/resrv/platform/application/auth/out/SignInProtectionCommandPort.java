package io.resrv.platform.application.auth.out;

import io.resrv.platform.domain.account.AccountSignInProtection;
import io.resrv.platform.domain.account.PasswordResetChallenge;
import io.resrv.shared.kernel.AccountId;
import java.time.Instant;

public interface SignInProtectionCommandPort {

    AccountSignInProtection recordFailedPasswordAttempt(AccountId accountId, Instant occurredAt);

    void clearProtection(AccountId accountId, Instant updatedAt);

    PasswordResetChallenge createPasswordResetChallenge(PasswordResetChallenge challenge);

    void replaceActivePasswordResetChallenges(AccountId accountId, Instant replacedAt);

    void markPasswordResetChallengeUsed(PasswordResetChallenge challenge, Instant usedAt);
}
