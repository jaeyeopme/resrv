package io.resrv.adapter.in.web.auth;

import io.resrv.adapter.in.web.auth.dto.AuthMeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Tag(name = "Authentication", description = "Authenticated identity and token revocation")
@SecurityRequirement(name = "bearerAuth")
interface AuthApiDocs {

    @Operation(
            summary = "Logout current token",
            description =
                    "Adds the current JWT JTI to the persistent revocation blacklist until the token expires.")
    @ApiResponse(responseCode = "204", description = "Token revoked")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> logout(@Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "Get current identity",
            description = "Returns user, tenant, and role claims from the current Bearer token.")
    @ApiResponse(responseCode = "200", description = "Current identity")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<AuthMeResponse> me(
            @Parameter(hidden = true) JwtAuthenticationToken authentication);
}
