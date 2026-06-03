package io.resrv.timeslot.adapter.in.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface ResourceApiDocs {

    @Operation(
            summary = "Create resource",
            responses = {
                @ApiResponse(responseCode = "201", description = "Resource created"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden")
            })
    ResponseEntity<ResourceWebAdapter.ResourceResponse> create(
            UUID businessId,
            JwtAuthenticationToken authentication,
            @Valid ResourceWebAdapter.ResourceRequest request);

    @Operation(
            summary = "Replace resource details and booking overrides",
            responses = {
                @ApiResponse(responseCode = "200", description = "Resource replaced"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Resource not found")
            })
    ResourceWebAdapter.ResourceResponse replace(
            UUID businessId,
            UUID resourceId,
            JwtAuthenticationToken authentication,
            @Valid ResourceWebAdapter.ResourceRequest request);

    @Operation(
            summary = "Activate resource",
            responses = {
                @ApiResponse(responseCode = "200", description = "Resource activated"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Resource not found")
            })
    ResourceWebAdapter.ResourceResponse activate(
            UUID businessId, UUID resourceId, JwtAuthenticationToken authentication);

    @Operation(
            summary = "Deactivate resource",
            responses = {
                @ApiResponse(responseCode = "200", description = "Resource deactivated"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Resource not found")
            })
    ResourceWebAdapter.ResourceResponse deactivate(
            UUID businessId, UUID resourceId, JwtAuthenticationToken authentication);

    @Operation(
            summary = "List public bookable resources",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description =
                                "Active resources for an active business, or an empty list when not bookable")
            })
    List<ResourceWebAdapter.ResourceResponse> list(UUID businessId);
}
