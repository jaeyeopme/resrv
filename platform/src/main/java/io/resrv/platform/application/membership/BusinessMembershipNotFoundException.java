package io.resrv.platform.application.membership;

public final class BusinessMembershipNotFoundException extends RuntimeException {

    public BusinessMembershipNotFoundException() {
        super("Business membership not found");
    }
}
