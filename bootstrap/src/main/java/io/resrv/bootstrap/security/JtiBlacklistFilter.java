package io.resrv.bootstrap.security;

import io.resrv.application.auth.out.TokenRevocationPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

class JtiBlacklistFilter extends OncePerRequestFilter {

    private final TokenRevocationPort tokenRevocationPort;
    private final ObjectMapper objectMapper;

    JtiBlacklistFilter(
            final TokenRevocationPort tokenRevocationPort, final ObjectMapper objectMapper) {
        this.tokenRevocationPort = tokenRevocationPort;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain)
            throws ServletException, IOException {

        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth
                && tokenRevocationPort.isRevoked(jwtAuth.getToken().getId())) {
            writeUnauthorized(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(
            final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final var problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setDetail("Invalid or missing authentication token");
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
