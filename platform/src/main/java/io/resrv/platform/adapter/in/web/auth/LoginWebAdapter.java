package io.resrv.platform.adapter.in.web.auth;

import io.resrv.platform.application.auth.AuthenticationFailedException;
import io.resrv.platform.application.auth.in.LoginCommand;
import io.resrv.platform.application.auth.in.LoginResult;
import io.resrv.platform.application.auth.in.LoginUseCase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class LoginWebAdapter implements LoginApiDocs {

    private final LoginUseCase loginUseCase;

    LoginWebAdapter(final LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
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
