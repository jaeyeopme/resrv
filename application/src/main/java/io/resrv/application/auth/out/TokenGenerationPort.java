package io.resrv.application.auth.out;

import java.util.UUID;

public interface TokenGenerationPort {

    TokenResult generate(final UUID userId, final UUID tenantId, final String role);
}
