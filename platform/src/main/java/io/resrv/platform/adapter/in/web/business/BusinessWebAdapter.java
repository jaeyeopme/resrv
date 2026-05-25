package io.resrv.platform.adapter.in.web.business;

import io.resrv.platform.adapter.in.web.security.AuthenticatedAccount;
import io.resrv.platform.application.business.in.CreateBusinessCommand;
import io.resrv.platform.application.business.in.CreateBusinessResult;
import io.resrv.platform.application.business.in.CreateBusinessUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses")
class BusinessWebAdapter {

    private final CreateBusinessUseCase createBusinessUseCase;

    BusinessWebAdapter(final CreateBusinessUseCase createBusinessUseCase) {
        this.createBusinessUseCase = createBusinessUseCase;
    }

    @PostMapping
    ResponseEntity<BusinessResponse> create(
            final JwtAuthenticationToken authentication,
            @Valid @RequestBody final BusinessRequest request) {
        final var account = AuthenticatedAccount.from(authentication);
        final var result =
                createBusinessUseCase.create(
                        new CreateBusinessCommand(
                                account.accountId(),
                                request.name(),
                                request.slug(),
                                request.timezone()));
        return ResponseEntity.created(URI.create("/api/businesses/" + result.id()))
                .body(BusinessResponse.from(result));
    }

    record BusinessRequest(
            @NotBlank(message = "Name is required")
                    @Size(max = 100, message = "Name must be 1-100 characters")
                    String name,
            @NotBlank(message = "Slug is required") String slug,
            @NotBlank(message = "Timezone is required") String timezone) {}

    record BusinessResponse(UUID id, String name, String slug, String timezone) {

        static BusinessResponse from(final CreateBusinessResult result) {
            return new BusinessResponse(
                    result.id(), result.name(), result.slug(), result.timezone());
        }
    }
}
