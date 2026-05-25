package io.resrv.timeslot.application.settings;

import io.resrv.timeslot.application.business.BusinessNotAvailableException;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.settings.in.BusinessBookingSettingsResult;
import io.resrv.timeslot.application.settings.in.UpsertBusinessBookingSettingsCommand;
import io.resrv.timeslot.application.settings.in.UpsertBusinessBookingSettingsUseCase;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsCommandPort;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessBookingSettingsService implements UpsertBusinessBookingSettingsUseCase {

    private final BusinessLookupPort businessLookupPort;
    private final BusinessBookingSettingsCommandPort commandPort;
    private final BusinessBookingSettingsQueryPort queryPort;
    private final Clock clock;

    public BusinessBookingSettingsService(
            final BusinessLookupPort businessLookupPort,
            final BusinessBookingSettingsCommandPort commandPort,
            final BusinessBookingSettingsQueryPort queryPort,
            final Clock clock) {
        this.businessLookupPort = businessLookupPort;
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.clock = clock;
    }

    @Override
    public BusinessBookingSettingsResult upsert(
            final UpsertBusinessBookingSettingsCommand command) {
        if (businessLookupPort.findActiveById(command.businessId()).isEmpty()) {
            throw new BusinessNotAvailableException(command.businessId());
        }

        final var slotDuration = new SlotDuration(command.slotDurationMinutes());
        final var holdTtl = new HoldTtl(command.holdTtlMinutes());
        final var cancellationWindow = new CancellationWindow(command.cancellationWindowMinutes());
        final var maxAdvanceBookingDays =
                new MaxAdvanceBookingDays(command.maxAdvanceBookingDays());
        final var now = clock.instant();

        final var existing = queryPort.findByBusinessId(command.businessId());
        final var settings =
                existing.map(
                                value ->
                                        value.update(
                                                slotDuration,
                                                holdTtl,
                                                cancellationWindow,
                                                maxAdvanceBookingDays,
                                                now))
                        .orElseGet(
                                () ->
                                        BusinessBookingSettings.create(
                                                command.businessId(),
                                                slotDuration,
                                                holdTtl,
                                                cancellationWindow,
                                                maxAdvanceBookingDays,
                                                now));
        commandPort.save(settings);
        return BusinessBookingSettingsResult.from(settings);
    }
}
