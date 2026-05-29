package io.resrv.timeslot.adapter.in.web.reservation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface ReservationApiDocs {

    @Operation(
            summary = "Hold reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation hold created"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Business access is required"),
                @ApiResponse(responseCode = "404", description = "Business or resource not found"),
                @ApiResponse(responseCode = "422", description = "Slot unavailable")
            })
    ReservationWebAdapter.ReservationResponse hold(
            UUID businessId,
            JwtAuthenticationToken authentication,
            @Valid ReservationWebAdapter.HoldRequest request);

    @Operation(
            summary = "List business reservations",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservations returned"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Business access is required"),
                @ApiResponse(responseCode = "404", description = "Business not found")
            })
    List<ReservationWebAdapter.ReservationResponse> list(
            UUID businessId,
            JwtAuthenticationToken authentication,
            LocalDate date,
            UUID resourceId,
            UUID customerAccountId,
            String state);

    @Operation(
            summary = "Confirm reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation confirmed"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(responseCode = "422", description = "Reservation cannot be confirmed")
            })
    ReservationWebAdapter.ReservationResponse confirm(
            UUID businessId, UUID reservationId, JwtAuthenticationToken authentication);

    @Operation(
            summary = "Release reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation released"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(responseCode = "422", description = "Reservation cannot be released")
            })
    ReservationWebAdapter.ReservationResponse release(
            UUID businessId, UUID reservationId, JwtAuthenticationToken authentication);

    @Operation(
            summary = "Cancel reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation cancelled"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Reservation access is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(responseCode = "422", description = "Reservation cannot be cancelled")
            })
    ReservationWebAdapter.ReservationResponse cancel(
            UUID businessId,
            UUID reservationId,
            JwtAuthenticationToken authentication,
            ReservationWebAdapter.CancelRequest request);

    @Operation(
            summary = "Check in reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation checked in"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Reservation access is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(responseCode = "422", description = "Reservation cannot be checked in")
            })
    ReservationWebAdapter.ReservationResponse checkIn(
            UUID businessId, UUID reservationId, JwtAuthenticationToken authentication);

    @Operation(
            summary = "Mark reservation no-show",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation marked no-show"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Reservation access is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(
                        responseCode = "422",
                        description = "Reservation cannot be marked no-show")
            })
    ReservationWebAdapter.ReservationResponse noShow(
            UUID businessId, UUID reservationId, JwtAuthenticationToken authentication);
}
