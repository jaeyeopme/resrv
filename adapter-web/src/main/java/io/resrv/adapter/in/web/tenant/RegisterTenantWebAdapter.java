package io.resrv.adapter.in.web.tenant;

import io.resrv.application.tenant.in.RegisterTenantCommand;
import io.resrv.application.tenant.in.RegisterTenantUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
class RegisterTenantWebAdapter implements RegisterTenantApiDocs {

    private final RegisterTenantUseCase registerTenantUseCase;

    RegisterTenantWebAdapter(final RegisterTenantUseCase registerTenantUseCase) {
        this.registerTenantUseCase = registerTenantUseCase;
    }

    @Override
    @PostMapping
    public ResponseEntity<TenantResponse> register(
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
