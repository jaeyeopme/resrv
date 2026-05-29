package io.resrv.platform.application.membership;

public final class DuplicateActiveMembershipException extends RuntimeException {

    public DuplicateActiveMembershipException() {
        super("Active business membership already exists");
    }
}
