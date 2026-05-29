package io.resrv.timeslot.adapter.in.web.discovery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface PublicBookingDiscoveryApiDocs {

    @Operation(
            summary = "Discover public bookable business",
            responses = {
                @ApiResponse(responseCode = "200", description = "Public business summary"),
                @ApiResponse(responseCode = "400", description = "Malformed business slug"),
                @ApiResponse(
                        responseCode = "404",
                        description = "No public bookable business representation")
            })
    PublicBookingDiscoveryWebAdapter.PublicBusinessResponse business(
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$") String businessSlug);

    @Operation(
            summary = "List public bookable resources",
            responses = {
                @ApiResponse(responseCode = "200", description = "Public resource list"),
                @ApiResponse(responseCode = "400", description = "Malformed business slug"),
                @ApiResponse(
                        responseCode = "404",
                        description = "No public bookable business representation")
            })
    List<PublicBookingDiscoveryWebAdapter.PublicResourceResponse> resources(
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$") String businessSlug);

    @Operation(
            summary = "List public schedule-derived slots",
            responses = {
                @ApiResponse(responseCode = "200", description = "Public slot list"),
                @ApiResponse(responseCode = "400", description = "Malformed path or query input"),
                @ApiResponse(
                        responseCode = "404",
                        description = "No public bookable resource representation")
            })
    List<PublicBookingDiscoveryWebAdapter.PublicSlotResponse> slots(
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$") String businessSlug,
            UUID resourceId,
            LocalDate date);

    @Operation(
            summary = "Create public booking hold",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation hold created"),
                @ApiResponse(responseCode = "400", description = "Malformed request"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(
                        responseCode = "404",
                        description = "No public bookable business representation"),
                @ApiResponse(responseCode = "422", description = "Slot unavailable")
            })
    PublicBookingDiscoveryWebAdapter.PublicReservationResponse hold(
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$") String businessSlug,
            JwtAuthenticationToken authentication,
            @Valid PublicBookingDiscoveryWebAdapter.HoldRequest request);
}
