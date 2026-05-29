package io.resrv.platform.adapter.in.web.auth;

import io.resrv.platform.application.auth.in.ResetPasswordCommand;
import io.resrv.platform.application.auth.in.ResetPasswordResult;
import io.resrv.platform.application.auth.in.ResetPasswordUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
class PasswordResetWebAdapter implements PasswordResetApiDocs {

    private final ResetPasswordUseCase resetPasswordUseCase;

    PasswordResetWebAdapter(final ResetPasswordUseCase resetPasswordUseCase) {
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @Override
    @PostMapping
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @Valid @RequestBody final ResetPasswordRequest request) {
        final var result =
                resetPasswordUseCase.resetPassword(
                        new ResetPasswordCommand(request.token(), request.newPassword()));
        return ResponseEntity.ok(ResetPasswordResponse.from(result));
    }

    record ResetPasswordRequest(
            @NotBlank(message = "Reset token is required") String token,
            @NotBlank(message = "New password is required")
                    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
                    String newPassword) {}

    record ResetPasswordResponse(boolean reset) {

        static ResetPasswordResponse from(final ResetPasswordResult result) {
            return new ResetPasswordResponse(result.reset());
        }
    }
}
