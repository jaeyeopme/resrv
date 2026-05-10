package io.resrv.adapter.in.web.auth;

import io.resrv.adapter.in.web.auth.dto.LoginRequest;
import io.resrv.adapter.in.web.auth.dto.LoginResponse;
import io.resrv.application.auth.AuthenticationFailedException;
import io.resrv.application.auth.in.LoginCommand;
import io.resrv.application.auth.in.LoginUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/{tenantSlug}/auth")
@Tag(name = "Authentication", description = "Public tenant administrator authentication")
class LoginWebAdapter {

    private final LoginUseCase loginUseCase;

    LoginWebAdapter(final LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @Operation(
            summary = "Log in as a tenant administrator",
            description =
                    "Authenticates an OWNER or STAFF account for the tenant identified by the URL slug.")
    @ApiResponse(responseCode = "200", description = "JWT issued")
    @ApiResponse(
            responseCode = "401",
            description = "Invalid tenant slug, credentials, or malformed login body",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(
            @Parameter(description = "Public tenant slug.", example = "demo-studio") @PathVariable
                    final String tenantSlug,
            @RequestBody final LoginRequest request) {
        final var command = new LoginCommand(tenantSlug, request.email(), request.password());
        final var result = loginUseCase.login(command);
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformedLoginBody(final HttpServletRequest request) {
        final var problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED, AuthenticationFailedException.MESSAGE);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}
