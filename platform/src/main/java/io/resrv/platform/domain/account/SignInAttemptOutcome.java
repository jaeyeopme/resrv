package io.resrv.platform.domain.account;

public enum SignInAttemptOutcome {
    FAILED_UNKNOWN_ACCOUNT,
    FAILED_BAD_PASSWORD,
    FAILED_REQUIRES_RESET,
    SUCCESS
}
