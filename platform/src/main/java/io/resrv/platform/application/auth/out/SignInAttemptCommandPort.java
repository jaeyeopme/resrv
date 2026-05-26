package io.resrv.platform.application.auth.out;

import io.resrv.platform.domain.account.SignInAttemptOutcome;
import io.resrv.shared.kernel.AccountId;
import java.time.Instant;
import java.util.Optional;

public interface SignInAttemptCommandPort {

    void recordAttempt(
            Optional<AccountId> accountId,
            String emailHash,
            String callerFingerprint,
            SignInAttemptOutcome outcome,
            Instant occurredAt);
}
