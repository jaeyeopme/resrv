package io.resrv.bootstrap.security;

import io.resrv.application.security.out.PasswordHashingPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class PasswordHashingAdapter implements PasswordHashingPort {

    private final PasswordEncoder passwordEncoder;

    PasswordHashingAdapter(final PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(final String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(final String rawPassword, final String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
