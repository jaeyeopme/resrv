package io.resrv.adapter.in.web.reservation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.ProblemDetail;

@ApiResponse(
        responseCode = "400",
        description = "Invalid reservation id",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(
        responseCode = "401",
        description = "Missing or invalid Bearer token",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(
        responseCode = "403",
        description = "Authenticated principal is not an administrator",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(
        responseCode = "404",
        description = "Reservation not found in the authenticated tenant",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(
        responseCode = "409",
        description = "Reservation cannot transition from its current state",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface AdminReservationErrorResponses {}
