package io.resrv.timeslot.adapter.in.web.settings;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.adapter.in.web.security.BusinessAccessGuard;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.settings.in.BusinessBookingSettingsResult;
import io.resrv.timeslot.application.settings.in.UpsertBusinessBookingSettingsCommand;
import io.resrv.timeslot.application.settings.in.UpsertBusinessBookingSettingsUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}/booking-settings")
class BusinessBookingSettingsWebAdapter {

    private final UpsertBusinessBookingSettingsUseCase useCase;
    private final BusinessAccessPort businessAccessPort;

    BusinessBookingSettingsWebAdapter(
            final UpsertBusinessBookingSettingsUseCase useCase,
            final BusinessAccessPort businessAccessPort) {
        this.useCase = useCase;
        this.businessAccessPort = businessAccessPort;
    }

    @PutMapping
    SettingsResponse upsert(
            @PathVariable final UUID businessId,
            final JwtAuthenticationToken authentication,
            @Valid @RequestBody final SettingsRequest request) {
        BusinessAccessGuard.requireAccess(businessAccessPort, authentication, businessId);
        return SettingsResponse.from(
                useCase.upsert(
                        new UpsertBusinessBookingSettingsCommand(
                                BusinessId.of(businessId),
                                request.slotDurationMinutes(),
                                request.holdTtlMinutes(),
                                request.cancellationWindowMinutes(),
                                request.maxAdvanceBookingDays())));
    }

    record SettingsRequest(
            @Min(5) @Max(480) int slotDurationMinutes,
            @Min(1) @Max(30) int holdTtlMinutes,
            @Min(0) @Max(10080) int cancellationWindowMinutes,
            @Min(1) @Max(365) int maxAdvanceBookingDays) {}

    record SettingsResponse(
            UUID businessId,
            int slotDurationMinutes,
            int holdTtlMinutes,
            int cancellationWindowMinutes,
            int maxAdvanceBookingDays) {

        static SettingsResponse from(final BusinessBookingSettingsResult result) {
            return new SettingsResponse(
                    result.businessId(),
                    result.slotDurationMinutes(),
                    result.holdTtlMinutes(),
                    result.cancellationWindowMinutes(),
                    result.maxAdvanceBookingDays());
        }
    }
}
