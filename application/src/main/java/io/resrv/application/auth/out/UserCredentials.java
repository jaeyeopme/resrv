package io.resrv.application.auth.out;

import java.util.UUID;

public record UserCredentials(
        UUID userId, UUID tenantId, String hashedPassword, String role, boolean active) {}
