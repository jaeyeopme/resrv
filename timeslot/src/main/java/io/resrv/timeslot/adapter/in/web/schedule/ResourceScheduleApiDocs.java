package io.resrv.timeslot.adapter.in.web.schedule;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface ResourceScheduleApiDocs {

    @Operation(
            summary = "Replace weekly resource schedule",
            responses = {
                @ApiResponse(responseCode = "200", description = "Weekly schedule replaced"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Resource not found")
            })
    ResourceScheduleWebAdapter.ScheduleResponse replaceWeekly(
            UUID businessId,
            UUID resourceId,
            DayOfWeek dayOfWeek,
            JwtAuthenticationToken authentication,
            @Valid ResourceScheduleWebAdapter.ScheduleRequest request);

    @Operation(
            summary = "Replace resource date schedule override",
            responses = {
                @ApiResponse(responseCode = "200", description = "Date override replaced"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Resource not found")
            })
    ResourceScheduleWebAdapter.ScheduleResponse replaceDateOverride(
            UUID businessId,
            UUID resourceId,
            LocalDate date,
            JwtAuthenticationToken authentication,
            @Valid ResourceScheduleWebAdapter.ScheduleRequest request);
}
