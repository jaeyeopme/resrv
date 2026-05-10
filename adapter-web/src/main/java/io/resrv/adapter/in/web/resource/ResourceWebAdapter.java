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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Resources", description = "Tenant-scoped resource management")
@SecurityRequirement(name = "bearerAuth")
class ResourceWebAdapter {

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

    @Operation(
            summary = "Create a resource",
            description =
                    "Creates a tenant-scoped reservable resource. Tenant id comes from the JWT.")
    @ApiResponse(responseCode = "201", description = "Resource created")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid resource payload",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not an administrator",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Resource slug already exists in the tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<ResourceResponse> create(
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

    @Operation(
            summary = "List active resources",
            description = "Returns active resources for the authenticated tenant.")
    @ApiResponse(responseCode = "200", description = "Resources returned")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not an administrator",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    ResponseEntity<List<ResourceResponse>> list(final JwtAuthenticationToken authentication) {
        final var response =
                listResourcesUseCase
                        .list(new ListResourcesQuery(adminTenantId(authentication)))
                        .stream()
                        .map(ResourceResponse::from)
                        .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get a resource",
            description = "Returns one tenant-scoped resource by id.")
    @ApiResponse(responseCode = "200", description = "Resource returned")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not an administrator",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Resource not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{resourceId}")
    ResponseEntity<ResourceResponse> get(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID resourceId,
            final JwtAuthenticationToken authentication) {
        final var response =
                ResourceResponse.from(
                        getResourceUseCase.get(
                                new GetResourceQuery(
                                        adminTenantId(authentication), ResourceId.of(resourceId))));
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update a resource",
            description =
                    "Updates resource name, slug, and description within the authenticated tenant.")
    @ApiResponse(responseCode = "200", description = "Resource updated")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid resource payload",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not an administrator",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Resource not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Resource slug already exists in the tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{resourceId}")
    ResponseEntity<ResourceResponse> update(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID resourceId,
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

    @Operation(
            summary = "Deactivate a resource",
            description = "Marks a resource as INACTIVE instead of hard deleting it.")
    @ApiResponse(responseCode = "204", description = "Resource deactivated")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not an administrator",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Resource not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{resourceId}")
    ResponseEntity<Void> deactivate(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID resourceId,
            final JwtAuthenticationToken authentication) {
        deactivateResourceUseCase.deactivate(
                new DeactivateResourceCommand(
                        adminTenantId(authentication), ResourceId.of(resourceId)));
        return ResponseEntity.noContent().build();
    }

    private static TenantId adminTenantId(final JwtAuthenticationToken authentication) {
        return AuthenticatedPrincipal.from(authentication).requireAdmin().tenantId();
    }
}
