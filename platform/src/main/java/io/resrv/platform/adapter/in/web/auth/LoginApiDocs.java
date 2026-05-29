package io.resrv.platform.adapter.in.web.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

interface LoginApiDocs {

    @Operation(
            summary = "Sign in",
            responses = {
                @ApiResponse(responseCode = "200", description = "Sign-in succeeded"),
                @ApiResponse(responseCode = "400", description = "Malformed request"),
                @ApiResponse(
                        responseCode = "401",
                        description =
                                "Sign-in failed or password reset is required without account enumeration")
            })
    ResponseEntity<LoginWebAdapter.LoginResponse> login(
            LoginWebAdapter.LoginRequest request, HttpServletRequest servletRequest);
}
