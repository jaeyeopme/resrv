package io.resrv.platform.application.membership;

public final class MembershipAdministrationDeniedException extends RuntimeException {

    public MembershipAdministrationDeniedException() {
        super("Business membership not found");
    }
}
