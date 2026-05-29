package io.resrv.platform.application.membership;

public final class TargetAccountUnavailableException extends RuntimeException {

    public TargetAccountUnavailableException() {
        super("Target account cannot receive business membership");
    }
}
