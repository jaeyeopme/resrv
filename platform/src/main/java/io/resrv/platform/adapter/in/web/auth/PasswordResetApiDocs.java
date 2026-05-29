package io.resrv.platform.adapter.in.web.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

interface PasswordResetApiDocs {

    @Operation(
            summary = "Reset password",
            responses = {
                @ApiResponse(responseCode = "200", description = "Password reset succeeded"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Reset token, password, or request body is invalid")
            })
    ResponseEntity<PasswordResetWebAdapter.ResetPasswordResponse> resetPassword(
            @Valid PasswordResetWebAdapter.ResetPasswordRequest request);
}
