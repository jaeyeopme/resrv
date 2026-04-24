package io.resrv.adapter.in.web.security;

import static io.resrv.application.auth.TokenClaimNames.ROLE;
import static io.resrv.application.auth.TokenClaimNames.TENANT_ID;
import static io.resrv.application.auth.TokenClaimNames.USER_ID;

import io.resrv.application.auth.RoleNames;
import io.resrv.application.security.ForbiddenOperationException;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.tenant.TenantId;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record AuthenticatedPrincipal(UUID userId, TenantId tenantId, String role) {

    public static AuthenticatedPrincipal from(final JwtAuthenticationToken authentication) {
        final var token = authentication.getToken();
        return new AuthenticatedPrincipal(
                UUID.fromString(token.getClaimAsString(USER_ID)),
                TenantId.of(UUID.fromString(token.getClaimAsString(TENANT_ID))),
                token.getClaimAsString(ROLE));
    }

    public AuthenticatedPrincipal requireAdmin() {
        if (!RoleNames.isAdmin(role)) {
            throw new ForbiddenOperationException("Admin role is required");
        }
        return this;
    }

    public AuthenticatedPrincipal requireCustomer() {
        if (!RoleNames.CUSTOMER.equals(role)) {
            throw new ForbiddenOperationException("Customer role is required");
        }
        return this;
    }

    public CustomerId customerId() {
        return CustomerId.of(userId);
    }
}
