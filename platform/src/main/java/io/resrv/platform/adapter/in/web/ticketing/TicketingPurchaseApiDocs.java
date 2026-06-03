package io.resrv.platform.adapter.in.web.ticketing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface TicketingPurchaseApiDocs {

    @Operation(
            summary = "Confirm selected-seat ticket purchase",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Ticket purchase confirmed",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                TicketPurchaseResponse.class))),
                @ApiResponse(
                        responseCode = "200",
                        description = "Existing ticket purchase replayed from idempotency key",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                TicketPurchaseResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Validation failure or idempotency problem reason invalid_retry or"
                                        + " expired_key"),
                @ApiResponse(
                        responseCode = "409",
                        description = "Unavailable selected seats",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                TicketPurchaseResponse.class))),
                @ApiResponse(responseCode = "401", description = "Authentication is required")
            })
    ResponseEntity<TicketPurchaseResponse> confirm(
            JwtAuthenticationToken authentication,
            UUID ticketEventId,
            @Valid ConfirmTicketPurchaseRequest request);

    @Operation(
            summary = "List customer ticket history",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Completed ticket purchases for the authenticated customer",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                CustomerTicketHistoryResponse
                                                                        .class))),
                @ApiResponse(responseCode = "401", description = "Authentication is required")
            })
    CustomerTicketHistoryResponse customerHistory(JwtAuthenticationToken authentication);

    @Operation(
            summary = "List business ticket purchase activity",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description =
                                "Completed ticket purchases for an event the business actor can"
                                        + " access",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                BusinessTicketActivityResponse
                                                                        .class))),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(
                        responseCode = "404",
                        description =
                                "Ticket event not found, including events outside caller authority")
            })
    BusinessTicketActivityResponse businessActivity(
            JwtAuthenticationToken authentication, UUID ticketEventId);
}
