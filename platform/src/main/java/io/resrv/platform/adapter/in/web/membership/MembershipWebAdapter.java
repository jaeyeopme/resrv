package io.resrv.platform.adapter.in.web.membership;

import io.resrv.platform.adapter.in.web.security.AuthenticatedAccount;
import io.resrv.platform.application.membership.in.BusinessMembershipAdministrationUseCase;
import io.resrv.platform.application.membership.in.DisableMembershipCommand;
import io.resrv.platform.application.membership.in.GrantStaffMembershipCommand;
import io.resrv.platform.application.membership.in.ListBusinessMembershipsQuery;
import io.resrv.platform.application.membership.in.MembershipAuditHistoryQuery;
import io.resrv.platform.application.membership.in.UpdateMembershipRoleCommand;
import io.resrv.shared.kernel.BusinessId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/businesses/{businessId}/memberships")
class MembershipWebAdapter {

    private final BusinessMembershipAdministrationUseCase useCase;

    MembershipWebAdapter(final BusinessMembershipAdministrationUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(
            summary = "Grant staff membership",
            responses = {
                @ApiResponse(responseCode = "201", description = "Membership granted"),
                @ApiResponse(responseCode = "400", description = "Target account is unavailable"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Owner access is required"),
                @ApiResponse(responseCode = "409", description = "Active membership already exists")
            })
    @PostMapping
    ResponseEntity<MembershipResponse> grant(
            final JwtAuthenticationToken authentication,
            @PathVariable final UUID businessId,
            @Valid @RequestBody final GrantMembershipRequest request) {
        final var account = AuthenticatedAccount.from(authentication);
        final var response =
                useCase.grantStaff(
                        new GrantStaffMembershipCommand(
                                account.accountId(),
                                BusinessId.of(businessId),
                                request.targetAccountEmail()));
        return ResponseEntity.created(
                        URI.create(
                                "/api/businesses/%s/memberships/%s"
                                        .formatted(businessId, response.membershipId())))
                .body(MembershipResponse.from(response));
    }

    @Operation(
            summary = "List business memberships",
            responses = {
                @ApiResponse(responseCode = "200", description = "Memberships returned"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Owner access is required")
            })
    @GetMapping
    List<MembershipListResponse> list(
            final JwtAuthenticationToken authentication, @PathVariable final UUID businessId) {
        final var account = AuthenticatedAccount.from(authentication);
        return useCase
                .listMemberships(
                        new ListBusinessMembershipsQuery(
                                account.accountId(), BusinessId.of(businessId)))
                .stream()
                .map(MembershipListResponse::from)
                .toList();
    }

    @Operation(
            summary = "List membership audit history",
            responses = {
                @ApiResponse(responseCode = "200", description = "Audit history returned"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Owner access is required")
            })
    @GetMapping("/audit")
    List<MembershipAuditHistoryResponse> audit(
            final JwtAuthenticationToken authentication, @PathVariable final UUID businessId) {
        final var account = AuthenticatedAccount.from(authentication);
        return useCase
                .listAuditHistory(
                        new MembershipAuditHistoryQuery(
                                account.accountId(), BusinessId.of(businessId)))
                .stream()
                .map(MembershipAuditHistoryResponse::from)
                .toList();
    }

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
    @PutMapping("/{membershipId}")
    MembershipResponse updateRole(
            final JwtAuthenticationToken authentication,
            @PathVariable final UUID businessId,
            @PathVariable final UUID membershipId,
            @Valid @RequestBody final UpdateMembershipRoleRequest request) {
        final var account = AuthenticatedAccount.from(authentication);
        return MembershipResponse.from(
                useCase.updateRole(
                        new UpdateMembershipRoleCommand(
                                account.accountId(),
                                BusinessId.of(businessId),
                                membershipId,
                                request.role())));
    }

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
    @PostMapping("/{membershipId}/disable")
    MembershipResponse disable(
            final JwtAuthenticationToken authentication,
            @PathVariable final UUID businessId,
            @PathVariable final UUID membershipId) {
        final var account = AuthenticatedAccount.from(authentication);
        return MembershipResponse.from(
                useCase.disable(
                        new DisableMembershipCommand(
                                account.accountId(), BusinessId.of(businessId), membershipId)));
    }
}
