package io.resrv.adapter.in.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Tag(name = "Resources", description = "Tenant-scoped resource management")
@SecurityRequirement(name = "bearerAuth")
interface ResourceApiDocs {

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
    ResponseEntity<ResourceResponse> create(
            ResourceRequest request,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

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
    ResponseEntity<List<ResourceResponse>> list(
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

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
    ResponseEntity<ResourceResponse> get(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

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
    ResponseEntity<ResourceResponse> update(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            ResourceRequest request,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

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
    ResponseEntity<Void> deactivate(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);
}
