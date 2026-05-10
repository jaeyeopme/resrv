package io.resrv.adapter.in.web.customer;

import io.resrv.adapter.in.web.auth.dto.LoginResponse;
import io.resrv.application.customer.in.CustomerLoginCommand;
import io.resrv.application.customer.in.CustomerLoginUseCase;
import io.resrv.application.customer.in.CustomerResult;
import io.resrv.application.customer.in.RegisterCustomerCommand;
import io.resrv.application.customer.in.RegisterCustomerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/{tenantSlug}/customers")
@Tag(name = "Customers", description = "Public customer registration and login")
class CustomerWebAdapter {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final CustomerLoginUseCase customerLoginUseCase;

    CustomerWebAdapter(
            final RegisterCustomerUseCase registerCustomerUseCase,
            final CustomerLoginUseCase customerLoginUseCase) {
        this.registerCustomerUseCase = registerCustomerUseCase;
        this.customerLoginUseCase = customerLoginUseCase;
    }

    @Operation(
            summary = "Register a customer",
            description = "Creates a customer account for the tenant identified by the URL slug.")
    @ApiResponse(responseCode = "201", description = "Customer registered")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid customer registration payload",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Customer email already exists in the tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<CustomerResponse> register(
            @Parameter(description = "Public tenant slug.", example = "demo-studio") @PathVariable
                    final String tenantSlug,
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

    @Operation(
            summary = "Log in as a customer",
            description =
                    "Authenticates a customer account for the tenant identified by the URL slug.")
    @ApiResponse(responseCode = "200", description = "JWT issued")
    @ApiResponse(
            responseCode = "401",
            description = "Invalid tenant slug or customer credentials",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(
            @Parameter(description = "Public tenant slug.", example = "demo-studio") @PathVariable
                    final String tenantSlug,
            @RequestBody final CustomerLoginRequest request) {
        final var result =
                customerLoginUseCase.login(
                        new CustomerLoginCommand(tenantSlug, request.email(), request.password()));
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @Schema(description = "Customer registration payload.")
    record RegisterCustomerRequest(
            @Schema(
                            description = "Customer email within the tenant.",
                            example = "customer@example.com")
                    @NotBlank(message = "Customer email is required")
                    @Email(message = "Must be a valid email address")
                    String email,
            @Schema(description = "Customer display name.", example = "Jane Customer")
                    @NotBlank(message = "Customer name is required")
                    @Size(max = 100, message = "Customer name must be at most 100 characters")
                    String name,
            @Schema(
                            description = "Customer password.",
                            example = "password123",
                            accessMode = Schema.AccessMode.WRITE_ONLY)
                    @NotBlank(message = "Customer password is required")
                    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
                    String password) {}

    @Schema(description = "Customer login credentials.")
    record CustomerLoginRequest(
            @Schema(
                            description = "Customer email within the tenant.",
                            example = "customer@example.com",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    String email,
            @Schema(
                            description = "Customer password.",
                            example = "password123",
                            accessMode = Schema.AccessMode.WRITE_ONLY,
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    String password) {}

    @Schema(description = "Registered customer.")
    record CustomerResponse(
            @Schema(
                            description = "Customer identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID id,
            @Schema(
                            description = "Tenant identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91d")
                    UUID tenantId,
            @Schema(description = "Customer email.", example = "customer@example.com") String email,
            @Schema(description = "Customer display name.", example = "Jane Customer") String name,
            @Schema(
                            description = "Whether the customer can currently use the service.",
                            example = "true")
                    boolean active,
            @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                    Instant createdAt) {

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
