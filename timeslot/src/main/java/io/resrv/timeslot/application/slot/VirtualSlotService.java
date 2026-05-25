package io.resrv.timeslot.application.slot;

import io.resrv.timeslot.application.business.BusinessNotAvailableException;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.resource.ResourceNotAvailableException;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.application.settings.BookingSettingsRequiredException;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.application.slot.in.ListSlotsQuery;
import io.resrv.timeslot.application.slot.in.ListSlotsUseCase;
import io.resrv.timeslot.application.slot.in.SlotResult;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import io.resrv.timeslot.domain.slot.SlotGenerator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VirtualSlotService implements ListSlotsUseCase {

    private final BusinessLookupPort businessLookupPort;
    private final BusinessBookingSettingsQueryPort settingsQueryPort;
    private final ResourceQueryPort resourceQueryPort;
    private final ResourceScheduleQueryPort scheduleQueryPort;
    private final Clock clock;

    public VirtualSlotService(
            final BusinessLookupPort businessLookupPort,
            final BusinessBookingSettingsQueryPort settingsQueryPort,
            final ResourceQueryPort resourceQueryPort,
            final ResourceScheduleQueryPort scheduleQueryPort,
            final Clock clock) {
        this.businessLookupPort = businessLookupPort;
        this.settingsQueryPort = settingsQueryPort;
        this.resourceQueryPort = resourceQueryPort;
        this.scheduleQueryPort = scheduleQueryPort;
        this.clock = clock;
    }

    @Override
    public List<SlotResult> listSlots(final ListSlotsQuery query) {
        Objects.requireNonNull(query, "Query must not be null");
        final var business =
                businessLookupPort
                        .findActiveById(query.businessId())
                        .orElseThrow(() -> new BusinessNotAvailableException(query.businessId()));
        final var settings =
                settingsQueryPort
                        .findByBusinessId(query.businessId())
                        .orElseThrow(
                                () -> new BookingSettingsRequiredException(query.businessId()));
        final var resource =
                resourceQueryPort
                        .findByBusinessIdAndId(query.businessId(), query.resourceId())
                        .filter(value -> value.status() == ResourceStatus.ACTIVE)
                        .orElseThrow(
                                () ->
                                        new ResourceNotAvailableException(
                                                query.businessId(), query.resourceId()));
        final var policy = resource.bookingOverrides().resolve(settings);

        final var today = LocalDate.now(clock.withZone(business.timezone().value()));
        if (query.date().isBefore(today)
                || query.date().isAfter(today.plusDays(policy.maxAdvanceBookingDays().days()))) {
            return List.of();
        }

        final var windows =
                scheduleQueryPort
                        .findDateOverride(query.businessId(), query.resourceId(), query.date())
                        .map(value -> value.windows())
                        .orElseGet(
                                () ->
                                        scheduleQueryPort
                                                .findWeekly(
                                                        query.businessId(),
                                                        query.resourceId(),
                                                        query.date().getDayOfWeek())
                                                .map(value -> value.windows())
                                                .orElse(List.of()));
        return SlotGenerator.generate(
                        query.businessId(),
                        query.resourceId(),
                        business.timezone(),
                        query.date(),
                        policy.slotDuration(),
                        windows)
                .stream()
                .map(SlotResult::from)
                .toList();
    }
}
