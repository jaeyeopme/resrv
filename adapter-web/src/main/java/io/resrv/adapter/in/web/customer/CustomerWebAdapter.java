package io.resrv.adapter.in.web.customer;

import io.resrv.adapter.in.web.auth.dto.LoginResponse;
import io.resrv.application.customer.in.CustomerLoginCommand;
import io.resrv.application.customer.in.CustomerLoginUseCase;
import io.resrv.application.customer.in.CustomerResult;
import io.resrv.application.customer.in.RegisterCustomerCommand;
import io.resrv.application.customer.in.RegisterCustomerUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/{tenantSlug}/customers")
class CustomerWebAdapter {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final CustomerLoginUseCase customerLoginUseCase;

    CustomerWebAdapter(
            final RegisterCustomerUseCase registerCustomerUseCase,
            final CustomerLoginUseCase customerLoginUseCase) {
        this.registerCustomerUseCase = registerCustomerUseCase;
        this.customerLoginUseCase = customerLoginUseCase;
    }

    @PostMapping
    ResponseEntity<CustomerResponse> register(
            @PathVariable final String tenantSlug,
            @Valid @RequestBody final RegisterCustomerRequest request) {
        final var result =
                registerCustomerUseCase.register(
                        new RegisterCustomerCommand(
                                tenantSlug, request.email(), request.name(), request.password()));
        final var response = CustomerResponse.from(result);
        return ResponseEntity.created(
                        URI.create("/public/" + tenantSlug + "/customers/" + response.id()))
                .body(response);
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(
            @PathVariable final String tenantSlug,
            @RequestBody final CustomerLoginRequest request) {
        final var result =
                customerLoginUseCase.login(
                        new CustomerLoginCommand(tenantSlug, request.email(), request.password()));
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    record RegisterCustomerRequest(
            @NotBlank(message = "Customer email is required")
                    @Email(message = "Must be a valid email address")
                    String email,
            @NotBlank(message = "Customer name is required")
                    @Size(max = 100, message = "Customer name must be at most 100 characters")
                    String name,
            @NotBlank(message = "Customer password is required")
                    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
                    String password) {}

    record CustomerLoginRequest(String email, String password) {}

    record CustomerResponse(
            UUID id, UUID tenantId, String email, String name, boolean active, Instant createdAt) {

        static CustomerResponse from(final CustomerResult result) {
            return new CustomerResponse(
                    result.id(),
                    result.tenantId(),
                    result.email(),
                    result.name(),
                    result.active(),
                    result.createdAt());
        }
    }
}
