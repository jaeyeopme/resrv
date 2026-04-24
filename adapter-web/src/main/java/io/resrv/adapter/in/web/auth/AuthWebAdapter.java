package io.resrv.adapter.in.web.auth;

import static io.resrv.application.auth.TokenClaimNames.ROLE;
import static io.resrv.application.auth.TokenClaimNames.TENANT_ID;
import static io.resrv.application.auth.TokenClaimNames.USER_ID;

import io.resrv.adapter.in.web.auth.dto.AuthMeResponse;
import io.resrv.application.auth.in.LogoutCommand;
import io.resrv.application.auth.in.LogoutUseCase;
import java.time.Instant;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthWebAdapter {

    private final LogoutUseCase logoutUseCase;

    AuthWebAdapter(final LogoutUseCase logoutUseCase) {
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(final JwtAuthenticationToken authentication) {
        final var jwt = Objects.requireNonNull(authentication.getToken());
        final var expiration = Objects.requireNonNullElse(jwt.getExpiresAt(), Instant.EPOCH);
        logoutUseCase.logout(new LogoutCommand(jwt.getId(), expiration));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    ResponseEntity<AuthMeResponse> me(final JwtAuthenticationToken authentication) {
        final var jwt = Objects.requireNonNull(authentication.getToken());
        return ResponseEntity.ok(
                new AuthMeResponse(
                        jwt.getClaimAsString(USER_ID),
                        jwt.getClaimAsString(TENANT_ID),
                        jwt.getClaimAsString(ROLE)));
    }
}
