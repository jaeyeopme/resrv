package io.resrv.application.security;

public final class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(final String message) {
        super(message);
    }
}
