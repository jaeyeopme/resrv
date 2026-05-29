package io.resrv.timeslot.adapter.in.web.resource;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.adapter.in.web.security.BusinessAccessGuard;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.resource.in.ActivateResourceCommand;
import io.resrv.timeslot.application.resource.in.ActivateResourceUseCase;
import io.resrv.timeslot.application.resource.in.CreateResourceCommand;
import io.resrv.timeslot.application.resource.in.CreateResourceUseCase;
import io.resrv.timeslot.application.resource.in.DeactivateResourceCommand;
import io.resrv.timeslot.application.resource.in.DeactivateResourceUseCase;
import io.resrv.timeslot.application.resource.in.ListResourcesUseCase;
import io.resrv.timeslot.application.resource.in.ReplaceResourceDetailsCommand;
import io.resrv.timeslot.application.resource.in.ReplaceResourceDetailsUseCase;
import io.resrv.timeslot.application.resource.in.ResourceResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}/resources")
class ResourceWebAdapter implements ResourceApiDocs {

    private final CreateResourceUseCase createResourceUseCase;
    private final ListResourcesUseCase listResourcesUseCase;
    private final ReplaceResourceDetailsUseCase replaceResourceDetailsUseCase;
    private final ActivateResourceUseCase activateResourceUseCase;
    private final DeactivateResourceUseCase deactivateResourceUseCase;
    private final BusinessAccessPort businessAccessPort;

    ResourceWebAdapter(
            final CreateResourceUseCase createResourceUseCase,
            final ListResourcesUseCase listResourcesUseCase,
            final ReplaceResourceDetailsUseCase replaceResourceDetailsUseCase,
            final ActivateResourceUseCase activateResourceUseCase,
            final DeactivateResourceUseCase deactivateResourceUseCase,
            final BusinessAccessPort businessAccessPort) {
        this.createResourceUseCase = createResourceUseCase;
        this.listResourcesUseCase = listResourcesUseCase;
        this.replaceResourceDetailsUseCase = replaceResourceDetailsUseCase;
        this.activateResourceUseCase = activateResourceUseCase;
        this.deactivateResourceUseCase = deactivateResourceUseCase;
        this.businessAccessPort = businessAccessPort;
    }

    @Override
    @PostMapping
    public ResponseEntity<ResourceResponse> create(
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

    @Override
    @PutMapping("/{resourceId}")
    public ResourceResponse replace(
            @PathVariable final UUID businessId,
            @PathVariable final UUID resourceId,
            final JwtAuthenticationToken authentication,
            @Valid @RequestBody final ResourceRequest request) {
        BusinessAccessGuard.requireAccess(businessAccessPort, authentication, businessId);
        return ResourceResponse.from(
                replaceResourceDetailsUseCase.replaceDetails(
                        new ReplaceResourceDetailsCommand(
                                BusinessId.of(businessId),
                                ResourceId.of(resourceId),
                                request.name(),
                                request.slug(),
                                request.description(),
                                request.slotDurationMinutes(),
                                request.holdTtlMinutes(),
                                request.cancellationWindowMinutes())));
    }

    @Override
    @PostMapping("/{resourceId}/activate")
    public ResourceResponse activate(
            @PathVariable final UUID businessId,
            @PathVariable final UUID resourceId,
            final JwtAuthenticationToken authentication) {
        BusinessAccessGuard.requireAccess(businessAccessPort, authentication, businessId);
        return ResourceResponse.from(
                activateResourceUseCase.activate(
                        new ActivateResourceCommand(
                                BusinessId.of(businessId), ResourceId.of(resourceId))));
    }

    @Override
    @PostMapping("/{resourceId}/deactivate")
    public ResourceResponse deactivate(
            @PathVariable final UUID businessId,
            @PathVariable final UUID resourceId,
            final JwtAuthenticationToken authentication) {
        BusinessAccessGuard.requireAccess(businessAccessPort, authentication, businessId);
        return ResourceResponse.from(
                deactivateResourceUseCase.deactivate(
                        new DeactivateResourceCommand(
                                BusinessId.of(businessId), ResourceId.of(resourceId))));
    }

    @Override
    @GetMapping
    public List<ResourceResponse> list(@PathVariable final UUID businessId) {
        return listResourcesUseCase.listActive(BusinessId.of(businessId)).stream()
                .map(ResourceResponse::from)
                .toList();
    }

    record ResourceRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank String slug,
            @Size(max = 500) String description,
            @Min(5) @Max(480) Integer slotDurationMinutes,
            @Min(1) @Max(30) Integer holdTtlMinutes,
            @Min(0) @Max(10080) Integer cancellationWindowMinutes) {}

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
