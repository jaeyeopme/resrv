package io.resrv.application.auth.in;

import java.time.Instant;

public record LogoutCommand(String jti, Instant expiration) {}
