package io.resrv.platform.adapter.out.persistence;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public final class PersistenceConstraintViolation {

    private PersistenceConstraintViolation() {}

    public static boolean isCausedBy(final Throwable throwable, final String constraintName) {
        var cause = throwable;
        while (cause != null) {
            if (constraintName.equals(extractConstraintName(cause).orElse(null))) {
                return true;
            }
            final var message = cause.getMessage();
            if (message != null && message.contains(constraintName)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static Optional<String> extractConstraintName(final Throwable throwable) {
        final var direct = invokeNoArg(throwable, "getConstraintName");
        if (direct instanceof String constraintName) {
            return Optional.of(constraintName);
        }
        final var serverErrorMessage = invokeNoArg(throwable, "getServerErrorMessage");
        if (serverErrorMessage == null) {
            return Optional.empty();
        }
        final var serverConstraint = invokeNoArg(serverErrorMessage, "getConstraint");
        if (serverConstraint instanceof String constraintName) {
            return Optional.of(constraintName);
        }
        return Optional.empty();
    }

    private static Object invokeNoArg(final Object target, final String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (final IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException _) {
            return null;
        }
    }
}
