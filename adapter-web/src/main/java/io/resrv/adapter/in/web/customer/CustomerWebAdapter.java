package io.resrv.adapter.in.web.customer;

import io.resrv.adapter.in.web.auth.dto.LoginResponse;
import io.resrv.application.customer.in.CustomerLoginCommand;
import io.resrv.application.customer.in.CustomerLoginUseCase;
import io.resrv.application.customer.in.RegisterCustomerCommand;
import io.resrv.application.customer.in.RegisterCustomerUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/{tenantSlug}/customers")
class CustomerWebAdapter implements CustomerApiDocs {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final CustomerLoginUseCase customerLoginUseCase;

    CustomerWebAdapter(
            final RegisterCustomerUseCase registerCustomerUseCase,
            final CustomerLoginUseCase customerLoginUseCase) {
        this.registerCustomerUseCase = registerCustomerUseCase;
        this.customerLoginUseCase = customerLoginUseCase;
    }

    @Override
    @PostMapping
    public ResponseEntity<CustomerResponse> register(
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

    @Override
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @PathVariable final String tenantSlug,
            @RequestBody final CustomerLoginRequest request) {
        final var result =
                customerLoginUseCase.login(
                        new CustomerLoginCommand(tenantSlug, request.email(), request.password()));
        return ResponseEntity.ok(LoginResponse.from(result));
    }
}
