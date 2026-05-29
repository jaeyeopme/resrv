package io.resrv.timeslot.adapter.in.web.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface BusinessBookingSettingsApiDocs {

    @Operation(
            summary = "Replace business booking settings",
            responses = {
                @ApiResponse(responseCode = "200", description = "Booking settings replaced"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden")
            })
    BusinessBookingSettingsWebAdapter.SettingsResponse upsert(
            UUID businessId,
            JwtAuthenticationToken authentication,
            @Valid BusinessBookingSettingsWebAdapter.SettingsRequest request);
}
