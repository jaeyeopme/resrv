package io.resrv.adapter.in.web.auth.dto;

import io.resrv.application.auth.LoginResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token returned after a successful login.")
public record LoginResponse(
        @Schema(description = "Bearer access token.", example = "eyJhbGciOiJIUzI1NiJ9...")
                String accessToken,
        @Schema(description = "Access token lifetime in seconds.", example = "1800") long expiresIn,
        @Schema(description = "Token type for Authorization header usage.", example = "Bearer")
                String tokenType) {

    public static LoginResponse from(final LoginResult result) {
        return new LoginResponse(result.accessToken(), result.expiresIn(), "Bearer");
    }
}
