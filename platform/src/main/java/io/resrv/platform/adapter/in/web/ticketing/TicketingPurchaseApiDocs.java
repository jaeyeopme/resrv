package io.resrv.platform.adapter.in.web.ticketing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface TicketingPurchaseApiDocs {

    @Operation(
            summary = "Confirm selected-seat ticket purchase",
            responses = {
                @ApiResponse(responseCode = "201", description = "Ticket purchase confirmed"),
                @ApiResponse(
                        responseCode = "200",
                        description = "Existing ticket purchase returned"),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Selected seats are invalid or unavailable, idempotency key is"
                                        + " missing, invalid retry, or expired"),
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
                        description = "Customer ticket history returned"),
                @ApiResponse(responseCode = "401", description = "Authentication is required")
            })
    CustomerTicketHistoryResponse customerHistory(JwtAuthenticationToken authentication);

    @Operation(
            summary = "List business ticket purchase activity",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Business purchase activity returned"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Ticket event not found")
            })
    BusinessTicketActivityResponse businessActivity(
            JwtAuthenticationToken authentication, UUID ticketEventId);
}
