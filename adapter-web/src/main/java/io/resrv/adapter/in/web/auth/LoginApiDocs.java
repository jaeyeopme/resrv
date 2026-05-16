package io.resrv.adapter.in.web.auth;

import io.resrv.adapter.in.web.auth.dto.LoginRequest;
import io.resrv.adapter.in.web.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Authentication", description = "Public tenant administrator authentication")
interface LoginApiDocs {

    @Operation(
            summary = "Log in as a tenant administrator",
            description =
                    "Authenticates an OWNER or STAFF account for the tenant identified by the URL slug.")
    @ApiResponse(responseCode = "200", description = "JWT issued")
    @ApiResponse(
            responseCode = "401",
            description = "Invalid tenant slug, credentials, or malformed login body",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<LoginResponse> login(
            @Parameter(description = "Public tenant slug.", example = "demo-studio")
                    String tenantSlug,
            LoginRequest request);
}
