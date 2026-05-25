package io.resrv.platform.application.security.out;

public interface PasswordHashingPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
