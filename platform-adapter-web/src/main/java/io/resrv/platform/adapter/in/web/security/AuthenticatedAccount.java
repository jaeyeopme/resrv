package io.resrv.platform.adapter.in.web.security;

import io.resrv.shared.kernel.AccountId;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record AuthenticatedAccount(AccountId accountId) {

    private static final String ACCOUNT_ID = "accountId";

    public static AuthenticatedAccount from(final JwtAuthenticationToken authentication) {
        final var token = authentication.getToken();
        final var accountId = token.getClaimAsString(ACCOUNT_ID);
        final var value = accountId == null || accountId.isBlank() ? token.getSubject() : accountId;
        return new AuthenticatedAccount(AccountId.of(UUID.fromString(value)));
    }
}
