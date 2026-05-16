package io.resrv.adapter.in.web.tenant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Tenants", description = "Tenant onboarding")
interface RegisterTenantApiDocs {

    @Operation(
            summary = "Create a tenant",
            description = "Creates a tenant and its first OWNER administrator account.")
    @ApiResponse(responseCode = "201", description = "Tenant created")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid tenant registration payload",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Tenant slug or administrator email already exists",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<TenantResponse> register(RegisterTenantRequest request);
}
