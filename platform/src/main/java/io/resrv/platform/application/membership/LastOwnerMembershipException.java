package io.resrv.platform.application.membership;

public final class LastOwnerMembershipException extends RuntimeException {

    public LastOwnerMembershipException() {
        super("At least one active owner membership is required");
    }
}
