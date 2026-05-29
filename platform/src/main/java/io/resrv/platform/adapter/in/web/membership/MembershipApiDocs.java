package io.resrv.platform.adapter.in.web.membership;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface MembershipApiDocs {

    @Operation(
            summary = "Grant staff membership",
            responses = {
                @ApiResponse(responseCode = "201", description = "Membership granted"),
                @ApiResponse(responseCode = "400", description = "Target account is unavailable"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Owner access is required"),
                @ApiResponse(responseCode = "409", description = "Active membership already exists")
            })
    ResponseEntity<MembershipResponse> grant(
            JwtAuthenticationToken authentication,
            UUID businessId,
            @Valid GrantMembershipRequest request);

    @Operation(
            summary = "List business memberships",
            responses = {
                @ApiResponse(responseCode = "200", description = "Memberships returned"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Owner access is required")
            })
    List<MembershipListResponse> list(JwtAuthenticationToken authentication, UUID businessId);

    @Operation(
            summary = "List membership audit history",
            responses = {
                @ApiResponse(responseCode = "200", description = "Audit history returned"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Owner access is required")
            })
    List<MembershipAuditHistoryResponse> audit(
            JwtAuthenticationToken authentication, UUID businessId);

    @Operation(
            summary = "Update membership role",
            responses = {
                @ApiResponse(responseCode = "200", description = "Membership role updated"),
                @ApiResponse(responseCode = "400", description = "Requested role is invalid"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(
                        responseCode = "404",
                        description = "Owner access or membership not found"),
                @ApiResponse(
                        responseCode = "409",
                        description = "Last owner membership is protected")
            })
    MembershipResponse updateRole(
            JwtAuthenticationToken authentication,
            UUID businessId,
            UUID membershipId,
            @Valid UpdateMembershipRoleRequest request);

    @Operation(
            summary = "Disable membership",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Membership disabled or current inactive state returned"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(
                        responseCode = "404",
                        description = "Owner access or membership not found"),
                @ApiResponse(
                        responseCode = "409",
                        description = "Last owner membership is protected")
            })
    MembershipResponse disable(
            JwtAuthenticationToken authentication, UUID businessId, UUID membershipId);
}
