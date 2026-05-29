package io.resrv.platform.adapter.in.web.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

interface AccountApiDocs {

    @Operation(
            summary = "Register account",
            responses = {
                @ApiResponse(responseCode = "201", description = "Account registered"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "409", description = "Email already registered")
            })
    ResponseEntity<AccountWebAdapter.AccountResponse> register(
            @Valid AccountWebAdapter.AccountRequest request);
}
