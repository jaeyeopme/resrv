package io.resrv.adapter.in.web.auth;

import static io.resrv.application.auth.TokenClaimNames.ROLE;
import static io.resrv.application.auth.TokenClaimNames.TENANT_ID;
import static io.resrv.application.auth.TokenClaimNames.USER_ID;

import io.resrv.adapter.in.web.auth.dto.AuthMeResponse;
import io.resrv.application.auth.in.LogoutCommand;
import io.resrv.application.auth.in.LogoutUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Objects;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authenticated identity and token revocation")
@SecurityRequirement(name = "bearerAuth")
class AuthWebAdapter {

    private final LogoutUseCase logoutUseCase;

    AuthWebAdapter(final LogoutUseCase logoutUseCase) {
        this.logoutUseCase = logoutUseCase;
    }

    @Operation(
            summary = "Logout current token",
            description =
                    "Adds the current JWT JTI to the in-memory blacklist until the token expires.")
    @ApiResponse(responseCode = "204", description = "Token revoked")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/logout")
    ResponseEntity<Void> logout(final JwtAuthenticationToken authentication) {
        final var jwt = Objects.requireNonNull(authentication.getToken());
        final var expiration = Objects.requireNonNullElse(jwt.getExpiresAt(), Instant.EPOCH);
        logoutUseCase.logout(new LogoutCommand(jwt.getId(), expiration));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get current identity",
            description = "Returns user, tenant, and role claims from the current Bearer token.")
    @ApiResponse(responseCode = "200", description = "Current identity")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
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
