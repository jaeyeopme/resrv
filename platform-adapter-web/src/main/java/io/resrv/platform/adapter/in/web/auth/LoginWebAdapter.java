package io.resrv.platform.adapter.in.web.auth;

import io.resrv.platform.application.auth.in.LoginCommand;
import io.resrv.platform.application.auth.in.LoginResult;
import io.resrv.platform.application.auth.in.LoginUseCase;
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

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@RequestBody final LoginRequest request) {
        final var result =
                loginUseCase.login(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    record LoginRequest(String email, String password) {}

    record LoginResponse(String accessToken, long expiresIn) {

        static LoginResponse from(final LoginResult result) {
            return new LoginResponse(result.accessToken(), result.expiresIn());
        }
    }
}
