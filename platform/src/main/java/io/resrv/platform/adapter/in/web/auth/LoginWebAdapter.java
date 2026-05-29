package io.resrv.platform.adapter.in.web.auth;

import io.resrv.platform.application.auth.AuthenticationFailedException;
import io.resrv.platform.application.auth.in.LoginCommand;
import io.resrv.platform.application.auth.in.LoginResult;
import io.resrv.platform.application.auth.in.LoginUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class LoginWebAdapter {

    private final LoginUseCase loginUseCase;

    LoginWebAdapter(final LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

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
    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(
            @RequestBody final LoginRequest request, final HttpServletRequest servletRequest) {
        if (request == null) {
            throw new AuthenticationFailedException();
        }
        final var result =
                loginUseCase.login(
                        new LoginCommand(
                                request.email(),
                                request.password(),
                                callerFingerprint(servletRequest)));
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    private static String callerFingerprint(final HttpServletRequest request) {
        final var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    record LoginRequest(String email, String password) {}

    record LoginResponse(String accessToken, long expiresIn) {

        static LoginResponse from(final LoginResult result) {
            return new LoginResponse(result.accessToken(), result.expiresIn());
        }
    }
}
