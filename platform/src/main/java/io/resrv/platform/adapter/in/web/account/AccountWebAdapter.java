package io.resrv.platform.adapter.in.web.account;

import io.resrv.platform.application.account.in.RegisterAccountCommand;
import io.resrv.platform.application.account.in.RegisterAccountResult;
import io.resrv.platform.application.account.in.RegisterAccountUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
class AccountWebAdapter {

    private final RegisterAccountUseCase registerAccountUseCase;

    AccountWebAdapter(final RegisterAccountUseCase registerAccountUseCase) {
        this.registerAccountUseCase = registerAccountUseCase;
    }

    @PostMapping
    ResponseEntity<AccountResponse> register(@Valid @RequestBody final AccountRequest request) {
        final var result =
                registerAccountUseCase.register(
                        new RegisterAccountCommand(
                                request.email(), request.name(), request.password()));
        return ResponseEntity.created(URI.create("/api/accounts/" + result.id()))
                .body(AccountResponse.from(result));
    }

    record AccountRequest(
            @NotBlank(message = "Email is required") @Email(message = "Must be a valid email")
                    String email,
            @NotBlank(message = "Name is required")
                    @Size(max = 100, message = "Name must be 1-100 characters")
                    String name,
            @NotBlank(message = "Password is required")
                    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
                    String password) {}

    record AccountResponse(UUID id, String email, String name) {

        static AccountResponse from(final RegisterAccountResult result) {
            return new AccountResponse(result.id(), result.email(), result.name());
        }
    }
}
