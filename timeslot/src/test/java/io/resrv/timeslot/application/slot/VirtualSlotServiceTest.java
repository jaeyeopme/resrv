package io.resrv.timeslot.application.slot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.application.slot.in.ListSlotsQuery;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceBookingOverrides;
import io.resrv.timeslot.domain.resource.ResourceName;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class VirtualSlotServiceTest {

    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final Timezone TIMEZONE = Timezone.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-05-24T00:00:00Z");
    private static final LocalDate DATE = LocalDate.parse("2026-05-25");

    private BusinessLookupPort businessLookupPort;
    private BusinessBookingSettingsQueryPort settingsQueryPort;
    private ResourceQueryPort resourceQueryPort;
    private ResourceScheduleQueryPort scheduleQueryPort;
    private VirtualSlotService service;

    @BeforeEach
    void setUp() {
        businessLookupPort = mock(BusinessLookupPort.class);
        settingsQueryPort = mock(BusinessBookingSettingsQueryPort.class);
        resourceQueryPort = mock(ResourceQueryPort.class);
        scheduleQueryPort = mock(ResourceScheduleQueryPort.class);
        service =
                new VirtualSlotService(
                        businessLookupPort,
                        settingsQueryPort,
                        resourceQueryPort,
                        scheduleQueryPort,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listSlotsUsesDateOverrideBeforeWeeklySchedule() {
        givenBusinessSettingsAndResource(ResourceBookingOverrides.none());
        when(scheduleQueryPort.findDateOverride(BUSINESS_ID, RESOURCE_ID, DATE))
                .thenReturn(
                        Optional.of(
                                DateResourceScheduleOverride.create(
                                        BUSINESS_ID,
                                        RESOURCE_ID,
                                        DATE,
                                        List.of(
                                                new ScheduleWindow(
                                                        LocalTime.of(14, 0), LocalTime.of(15, 0))),
                                        NOW)));
        when(scheduleQueryPort.findWeekly(BUSINESS_ID, RESOURCE_ID, DayOfWeek.MONDAY))
                .thenReturn(
                        Optional.of(
                                WeeklyResourceSchedule.create(
                                        BUSINESS_ID,
                                        RESOURCE_ID,
                                        DayOfWeek.MONDAY,
                                        List.of(
                                                new ScheduleWindow(
                                                        LocalTime.of(9, 0), LocalTime.of(10, 0))),
                                        NOW)));

        final var slots = service.listSlots(new ListSlotsQuery(BUSINESS_ID, RESOURCE_ID, DATE));

        assertEquals(2, slots.size());
        assertEquals("2026-05-25T14:00+09:00", slots.getFirst().startAtBusinessTime().toString());
    }

    @Test
    void listSlotsUsesResourceSlotDurationOverride() {
        givenBusinessSettingsAndResource(
                new ResourceBookingOverrides(
                        new SlotDuration(60), new HoldTtl(5), new CancellationWindow(120)));
        when(scheduleQueryPort.findDateOverride(BUSINESS_ID, RESOURCE_ID, DATE))
                .thenReturn(Optional.empty());
        when(scheduleQueryPort.findWeekly(BUSINESS_ID, RESOURCE_ID, DayOfWeek.MONDAY))
                .thenReturn(
                        Optional.of(
                                WeeklyResourceSchedule.create(
                                        BUSINESS_ID,
                                        RESOURCE_ID,
                                        DayOfWeek.MONDAY,
                                        List.of(
                                                new ScheduleWindow(
                                                        LocalTime.of(9, 0), LocalTime.of(11, 0))),
                                        NOW)));

        final var slots = service.listSlots(new ListSlotsQuery(BUSINESS_ID, RESOURCE_ID, DATE));

        assertEquals(2, slots.size());
        assertEquals("2026-05-25T09:00+09:00", slots.getFirst().startAtBusinessTime().toString());
        assertEquals("2026-05-25T10:00+09:00", slots.get(1).startAtBusinessTime().toString());
    }

    @Test
    void dateBeforeTodayInBusinessTimezoneReturnsEmpty() {
        givenBusinessSettingsAndResource(ResourceBookingOverrides.none());

        final var slots =
                service.listSlots(new ListSlotsQuery(BUSINESS_ID, RESOURCE_ID, DATE.minusDays(2)));

        assertEquals(List.of(), slots);
        verifyNoInteractions(scheduleQueryPort);
    }

    @Test
    void inactiveBusinessReturnsNoSlotsBeforeOtherPorts() {
        when(businessLookupPort.findActiveById(BUSINESS_ID)).thenReturn(Optional.empty());

        final var slots = service.listSlots(new ListSlotsQuery(BUSINESS_ID, RESOURCE_ID, DATE));

        assertEquals(List.of(), slots);
        verifyNoInteractions(settingsQueryPort, resourceQueryPort, scheduleQueryPort);
    }

    @Test
    void inactiveResourceReturnsNoSlots() {
        givenBusinessSettingsAndResource(
                Resource.reconstitute(
                        RESOURCE_ID,
                        BUSINESS_ID,
                        new ResourceName("Room A"),
                        null,
                        ResourceStatus.INACTIVE,
                        ResourceBookingOverrides.none(),
                        NOW,
                        NOW));

        final var slots = service.listSlots(new ListSlotsQuery(BUSINESS_ID, RESOURCE_ID, DATE));

        assertEquals(List.of(), slots);
        verifyNoInteractions(scheduleQueryPort);
    }

    @Test
    void listSlotsRejectsNullQuery() {
        assertThrows(NullPointerException.class, () -> service.listSlots(null));
    }

    private void givenBusinessSettingsAndResource(final ResourceBookingOverrides overrides) {
        givenBusinessSettingsAndResource(resource(overrides));
    }

    private void givenBusinessSettingsAndResource(final Resource resource) {
        when(businessLookupPort.findActiveById(BUSINESS_ID))
                .thenReturn(
                        Optional.of(
                                new BusinessLookupPort.BusinessView(
                                        BUSINESS_ID, "Salon A", "salon-a", TIMEZONE)));
        when(settingsQueryPort.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(settings()));
        when(resourceQueryPort.findByBusinessIdAndId(BUSINESS_ID, RESOURCE_ID))
                .thenReturn(Optional.of(resource));
    }

    private static BusinessBookingSettings settings() {
        return BusinessBookingSettings.create(
                BUSINESS_ID,
                new SlotDuration(30),
                new HoldTtl(10),
                new CancellationWindow(60),
                new MaxAdvanceBookingDays(30),
                NOW);
    }

    private static Resource resource(final ResourceBookingOverrides overrides) {
        return Resource.reconstitute(
                RESOURCE_ID,
                BUSINESS_ID,
                new ResourceName("Room A"),
                null,
                ResourceStatus.ACTIVE,
                overrides,
                NOW,
                NOW);
    }
}
