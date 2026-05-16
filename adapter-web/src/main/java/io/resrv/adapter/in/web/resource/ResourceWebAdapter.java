package io.resrv.adapter.in.web.resource;

import io.resrv.adapter.in.web.security.AuthenticatedPrincipal;
import io.resrv.application.resource.in.CreateResourceCommand;
import io.resrv.application.resource.in.CreateResourceUseCase;
import io.resrv.application.resource.in.DeactivateResourceCommand;
import io.resrv.application.resource.in.DeactivateResourceUseCase;
import io.resrv.application.resource.in.GetResourceQuery;
import io.resrv.application.resource.in.GetResourceUseCase;
import io.resrv.application.resource.in.ListResourcesQuery;
import io.resrv.application.resource.in.ListResourcesUseCase;
import io.resrv.application.resource.in.UpdateResourceCommand;
import io.resrv.application.resource.in.UpdateResourceUseCase;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
class ResourceWebAdapter implements ResourceApiDocs {

    private final CreateResourceUseCase createResourceUseCase;
    private final GetResourceUseCase getResourceUseCase;
    private final ListResourcesUseCase listResourcesUseCase;
    private final UpdateResourceUseCase updateResourceUseCase;
    private final DeactivateResourceUseCase deactivateResourceUseCase;

    ResourceWebAdapter(
            final CreateResourceUseCase createResourceUseCase,
            final GetResourceUseCase getResourceUseCase,
            final ListResourcesUseCase listResourcesUseCase,
            final UpdateResourceUseCase updateResourceUseCase,
            final DeactivateResourceUseCase deactivateResourceUseCase) {
        this.createResourceUseCase = createResourceUseCase;
        this.getResourceUseCase = getResourceUseCase;
        this.listResourcesUseCase = listResourcesUseCase;
        this.updateResourceUseCase = updateResourceUseCase;
        this.deactivateResourceUseCase = deactivateResourceUseCase;
    }

    @Override
    @PostMapping
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody final ResourceRequest request,
            final JwtAuthenticationToken authentication) {
        final var result =
                createResourceUseCase.create(
                        new CreateResourceCommand(
                                adminTenantId(authentication),
                                request.name(),
                                request.slug(),
                                request.description()));
        final var response = ResourceResponse.from(result);
        return ResponseEntity.created(URI.create("/api/resources/" + response.id())).body(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<ResourceResponse>> list(
            final JwtAuthenticationToken authentication) {
        final var response =
                listResourcesUseCase
                        .list(new ListResourcesQuery(adminTenantId(authentication)))
                        .stream()
                        .map(ResourceResponse::from)
                        .toList();
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{resourceId}")
    public ResponseEntity<ResourceResponse> get(
            @PathVariable final UUID resourceId, final JwtAuthenticationToken authentication) {
        final var response =
                ResourceResponse.from(
                        getResourceUseCase.get(
                                new GetResourceQuery(
                                        adminTenantId(authentication), ResourceId.of(resourceId))));
        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping("/{resourceId}")
    public ResponseEntity<ResourceResponse> update(
            @PathVariable final UUID resourceId,
            @Valid @RequestBody final ResourceRequest request,
            final JwtAuthenticationToken authentication) {
        final var response =
                ResourceResponse.from(
                        updateResourceUseCase.update(
                                new UpdateResourceCommand(
                                        adminTenantId(authentication),
                                        ResourceId.of(resourceId),
                                        request.name(),
                                        request.slug(),
                                        request.description())));
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> deactivate(
            @PathVariable final UUID resourceId, final JwtAuthenticationToken authentication) {
        deactivateResourceUseCase.deactivate(
                new DeactivateResourceCommand(
                        adminTenantId(authentication), ResourceId.of(resourceId)));
        return ResponseEntity.noContent().build();
    }

    private static TenantId adminTenantId(final JwtAuthenticationToken authentication) {
        return AuthenticatedPrincipal.from(authentication).requireAdmin().tenantId();
    }
}
