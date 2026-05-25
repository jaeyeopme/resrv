package io.resrv.timeslot.adapter.in.web.resource;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.adapter.in.web.security.BusinessAccessGuard;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.resource.in.CreateResourceCommand;
import io.resrv.timeslot.application.resource.in.CreateResourceUseCase;
import io.resrv.timeslot.application.resource.in.ListResourcesUseCase;
import io.resrv.timeslot.application.resource.in.ResourceResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}/resources")
class ResourceWebAdapter {

    private final CreateResourceUseCase createResourceUseCase;
    private final ListResourcesUseCase listResourcesUseCase;
    private final BusinessAccessPort businessAccessPort;

    ResourceWebAdapter(
            final CreateResourceUseCase createResourceUseCase,
            final ListResourcesUseCase listResourcesUseCase,
            final BusinessAccessPort businessAccessPort) {
        this.createResourceUseCase = createResourceUseCase;
        this.listResourcesUseCase = listResourcesUseCase;
        this.businessAccessPort = businessAccessPort;
    }

    @PostMapping
    ResponseEntity<ResourceResponse> create(
            @PathVariable final UUID businessId,
            final JwtAuthenticationToken authentication,
            @Valid @RequestBody final ResourceRequest request) {
        BusinessAccessGuard.requireAccess(businessAccessPort, authentication, businessId);
        final var result =
                createResourceUseCase.create(
                        new CreateResourceCommand(
                                BusinessId.of(businessId),
                                request.name(),
                                request.slug(),
                                request.description(),
                                request.slotDurationMinutes(),
                                request.holdTtlMinutes(),
                                request.cancellationWindowMinutes()));
        return ResponseEntity.created(
                        URI.create("/api/businesses/" + businessId + "/resources/" + result.id()))
                .body(ResourceResponse.from(result));
    }

    @GetMapping
    List<ResourceResponse> list(@PathVariable final UUID businessId) {
        return listResourcesUseCase.listActive(BusinessId.of(businessId)).stream()
                .map(ResourceResponse::from)
                .toList();
    }

    record ResourceRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank String slug,
            @Size(max = 500) String description,
            Integer slotDurationMinutes,
            Integer holdTtlMinutes,
            Integer cancellationWindowMinutes) {}

    record ResourceResponse(
            UUID id,
            UUID businessId,
            String name,
            String slug,
            String description,
            String status,
            Integer slotDurationMinutes,
            Integer holdTtlMinutes,
            Integer cancellationWindowMinutes) {

        static ResourceResponse from(final ResourceResult result) {
            return new ResourceResponse(
                    result.id(),
                    result.businessId(),
                    result.name(),
                    result.slug(),
                    result.description(),
                    result.status().name(),
                    result.slotDurationMinutes(),
                    result.holdTtlMinutes(),
                    result.cancellationWindowMinutes());
        }
    }
}
