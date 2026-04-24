package io.resrv.application.security.out;

public interface PasswordHashingPort {

    String hash(final String rawPassword);

    boolean matches(final String rawPassword, final String hashedPassword);
}
