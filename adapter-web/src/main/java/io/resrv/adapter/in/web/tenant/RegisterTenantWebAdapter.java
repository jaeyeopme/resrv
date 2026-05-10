package io.resrv.adapter.in.web.tenant;

import io.resrv.application.tenant.in.RegisterTenantCommand;
import io.resrv.application.tenant.in.RegisterTenantUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenants", description = "Tenant onboarding")
class RegisterTenantWebAdapter {

    private final RegisterTenantUseCase registerTenantUseCase;

    RegisterTenantWebAdapter(final RegisterTenantUseCase registerTenantUseCase) {
        this.registerTenantUseCase = registerTenantUseCase;
    }

    @Operation(
            summary = "Create a tenant",
            description = "Creates a tenant and its first OWNER administrator account.")
    @ApiResponse(responseCode = "201", description = "Tenant created")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid tenant registration payload",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Tenant slug or administrator email already exists",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<TenantResponse> register(
            @Valid @RequestBody final RegisterTenantRequest request) {
        final var command =
                new RegisterTenantCommand(
                        request.name(),
                        request.slug(),
                        request.timezone(),
                        request.slotDuration(),
                        request.holdTtl(),
                        request.cancellationWindow(),
                        request.admin().email(),
                        request.admin().password());

        final var tenant = registerTenantUseCase.register(command);
        final var response = TenantResponse.from(tenant);
        final var location = URI.create("/api/tenants/" + tenant.id().value());

        return ResponseEntity.created(location).body(response);
    }
}
