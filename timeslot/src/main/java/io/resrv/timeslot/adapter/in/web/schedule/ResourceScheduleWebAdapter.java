package io.resrv.timeslot.adapter.in.web.schedule;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.adapter.in.web.security.BusinessAccessGuard;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.schedule.ResourceScheduleService;
import io.resrv.timeslot.application.schedule.in.ReplaceDateScheduleOverrideCommand;
import io.resrv.timeslot.application.schedule.in.ReplaceWeeklyScheduleCommand;
import io.resrv.timeslot.application.schedule.in.ScheduleResult;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}/resources/{resourceId}")
class ResourceScheduleWebAdapter implements ResourceScheduleApiDocs {

    private final ResourceScheduleService service;
    private final BusinessAccessPort businessAccessPort;

    ResourceScheduleWebAdapter(
            final ResourceScheduleService service, final BusinessAccessPort businessAccessPort) {
        this.service = service;
        this.businessAccessPort = businessAccessPort;
    }

    @Override
    @PutMapping("/weekly-schedules/{dayOfWeek}")
    public ScheduleResponse replaceWeekly(
            @PathVariable final UUID businessId,
            @PathVariable final UUID resourceId,
            @PathVariable final DayOfWeek dayOfWeek,
            final JwtAuthenticationToken authentication,
            @Valid @RequestBody final ScheduleRequest request) {
        BusinessAccessGuard.requireAccess(businessAccessPort, authentication, businessId);
        return ScheduleResponse.from(
                service.replaceWeekly(
                        new ReplaceWeeklyScheduleCommand(
                                BusinessId.of(businessId),
                                ResourceId.of(resourceId),
                                dayOfWeek,
                                request.toWindows())));
    }

    @Override
    @PutMapping("/date-schedule-overrides/{date}")
    public ScheduleResponse replaceDateOverride(
            @PathVariable final UUID businessId,
            @PathVariable final UUID resourceId,
            @PathVariable final LocalDate date,
            final JwtAuthenticationToken authentication,
            @Valid @RequestBody final ScheduleRequest request) {
        BusinessAccessGuard.requireAccess(businessAccessPort, authentication, businessId);
        return ScheduleResponse.from(
                service.replaceDateOverride(
                        new ReplaceDateScheduleOverrideCommand(
                                BusinessId.of(businessId),
                                ResourceId.of(resourceId),
                                date,
                                request.toWindows())));
    }

    record ScheduleRequest(List<@Valid WindowRequest> windows) {

        ScheduleRequest {
            windows = windows == null ? List.of() : List.copyOf(windows);
        }

        List<ScheduleWindow> toWindows() {
            return windows.stream()
                    .map(window -> new ScheduleWindow(window.startTime(), window.endTime()))
                    .toList();
        }
    }

    record WindowRequest(@NotNull LocalTime startTime, @NotNull LocalTime endTime) {}

    record ScheduleResponse(UUID businessId, UUID resourceId, String dayOfWeek, LocalDate date) {

        static ScheduleResponse from(final ScheduleResult result) {
            return new ScheduleResponse(
                    result.businessId(),
                    result.resourceId(),
                    result.dayOfWeek() == null ? null : result.dayOfWeek().name(),
                    result.date());
        }
    }
}
