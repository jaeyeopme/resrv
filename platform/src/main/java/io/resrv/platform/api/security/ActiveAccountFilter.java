package io.resrv.platform.api.security;

import io.resrv.platform.contract.account.ActiveAccountCheck;
import io.resrv.shared.kernel.AccountId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class ActiveAccountFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ActiveAccountFilter.class);

    private final ActiveAccountCheck activeAccountCheck;

    ActiveAccountFilter(final ActiveAccountCheck activeAccountCheck) {
        this.activeAccountCheck = activeAccountCheck;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain)
            throws ServletException, IOException {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            final var accountId =
                    AccountId.of(
                            UUID.fromString(
                                    jwtAuthenticationToken
                                            .getToken()
                                            .getClaimAsString("accountId")));
            if (!activeAccountCheck.isActive(accountId)) {
                log.info(
                        "Denied protected request for inactive account: accountId={}, method={}, path={}",
                        accountId.value(),
                        request.getMethod(),
                        request.getRequestURI());
                response.sendError(HttpStatus.FORBIDDEN.value());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
