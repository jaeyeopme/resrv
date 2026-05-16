package io.resrv.adapter.in.web.customer;

import io.resrv.adapter.in.web.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Customers", description = "Public customer registration and login")
interface CustomerApiDocs {

    @Operation(
            summary = "Register a customer",
            description = "Creates a customer account for the tenant identified by the URL slug.")
    @ApiResponse(responseCode = "201", description = "Customer registered")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid customer registration payload",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Customer email already exists in the tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<CustomerResponse> register(
            @Parameter(description = "Public tenant slug.", example = "demo-studio")
                    String tenantSlug,
            RegisterCustomerRequest request);

    @Operation(
            summary = "Log in as a customer",
            description =
                    "Authenticates a customer account for the tenant identified by the URL slug.")
    @ApiResponse(responseCode = "200", description = "JWT issued")
    @ApiResponse(
            responseCode = "401",
            description = "Invalid tenant slug or customer credentials",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<LoginResponse> login(
            @Parameter(description = "Public tenant slug.", example = "demo-studio")
                    String tenantSlug,
            CustomerLoginRequest request);
}
