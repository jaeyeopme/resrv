package io.resrv.platform.adapter.in.web.business;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface BusinessApiDocs {

    @Operation(
            summary = "Create business",
            responses = {
                @ApiResponse(responseCode = "201", description = "Business created"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Active account is required"),
                @ApiResponse(responseCode = "409", description = "Business slug already exists")
            })
    ResponseEntity<BusinessWebAdapter.BusinessResponse> create(
            JwtAuthenticationToken authentication,
            @Valid BusinessWebAdapter.BusinessRequest request);
}
