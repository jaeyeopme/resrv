package io.resrv.timeslot.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.resource.ResourceNotAvailableException;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.in.ReplaceDateScheduleOverrideCommand;
import io.resrv.timeslot.application.schedule.in.ReplaceWeeklyScheduleCommand;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleCommandPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceBookingOverrides;
import io.resrv.timeslot.domain.resource.ResourceName;
import io.resrv.timeslot.domain.resource.ResourceSlug;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
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
import org.mockito.ArgumentCaptor;

class ResourceScheduleServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2025-12-01T00:00:00Z");
    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final LocalDate DATE = LocalDate.parse("2026-01-05");

    private ResourceQueryPort resourceQueryPort;
    private ResourceScheduleCommandPort commandPort;
    private ResourceScheduleQueryPort queryPort;
    private ResourceScheduleService service;

    @BeforeEach
    void setUp() {
        resourceQueryPort = mock(ResourceQueryPort.class);
        commandPort = mock(ResourceScheduleCommandPort.class);
        queryPort = mock(ResourceScheduleQueryPort.class);
        service =
                new ResourceScheduleService(
                        resourceQueryPort,
                        commandPort,
                        queryPort,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void replaceWeeklyVerifiesResourceAndSavesWindows() {
        final var windows =
                List.of(
                        new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new ScheduleWindow(LocalTime.of(13, 0), LocalTime.of(17, 0)));
        when(resourceQueryPort.findByBusinessIdAndId(BUSINESS_ID, RESOURCE_ID))
                .thenReturn(Optional.of(resource()));
        when(queryPort.findWeekly(BUSINESS_ID, RESOURCE_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.empty());

        final var result =
                service.replaceWeekly(
                        new ReplaceWeeklyScheduleCommand(
                                BUSINESS_ID, RESOURCE_ID, DayOfWeek.MONDAY, windows));

        assertEquals(BUSINESS_ID.value(), result.businessId());
        assertEquals(RESOURCE_ID.value(), result.resourceId());
        assertEquals(DayOfWeek.MONDAY, result.dayOfWeek());
        assertEquals(windows, result.windows());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.updatedAt());

        final var captor = ArgumentCaptor.forClass(WeeklyResourceSchedule.class);
        verify(commandPort).saveWeekly(captor.capture());
        final var saved = captor.getValue();
        assertEquals(BUSINESS_ID, saved.businessId());
        assertEquals(RESOURCE_ID, saved.resourceId());
        assertEquals(DayOfWeek.MONDAY, saved.dayOfWeek());
        assertEquals(windows, saved.windows());
        assertEquals(NOW, saved.createdAt());
        assertEquals(NOW, saved.updatedAt());
    }

    @Test
    void replaceDateOverrideAllowsEmptyWindowsForClosedDay() {
        when(resourceQueryPort.findByBusinessIdAndId(BUSINESS_ID, RESOURCE_ID))
                .thenReturn(Optional.of(resource()));
        when(queryPort.findDateOverride(BUSINESS_ID, RESOURCE_ID, DATE))
                .thenReturn(Optional.empty());

        final var result =
                service.replaceDateOverride(
                        new ReplaceDateScheduleOverrideCommand(
                                BUSINESS_ID, RESOURCE_ID, DATE, List.of()));

        assertEquals(BUSINESS_ID.value(), result.businessId());
        assertEquals(RESOURCE_ID.value(), result.resourceId());
        assertEquals(DATE, result.date());
        assertEquals(List.of(), result.windows());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.updatedAt());

        final var captor = ArgumentCaptor.forClass(DateResourceScheduleOverride.class);
        verify(commandPort).saveDateOverride(captor.capture());
        final var saved = captor.getValue();
        assertEquals(BUSINESS_ID, saved.businessId());
        assertEquals(RESOURCE_ID, saved.resourceId());
        assertEquals(DATE, saved.date());
        assertEquals(List.of(), saved.windows());
    }

    @Test
    void deleteDateOverrideVerifiesResourceAndDeletes() {
        when(resourceQueryPort.findByBusinessIdAndId(BUSINESS_ID, RESOURCE_ID))
                .thenReturn(Optional.of(resource()));

        service.deleteDateOverride(BUSINESS_ID, RESOURCE_ID, DATE);

        verify(commandPort).deleteDateOverride(BUSINESS_ID, RESOURCE_ID, DATE);
    }

    @Test
    void overlappingWeeklyWindowsFailBeforeSave() {
        final var windows =
                List.of(
                        new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new ScheduleWindow(LocalTime.of(11, 0), LocalTime.of(13, 0)));

        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.replaceWeekly(
                                        new ReplaceWeeklyScheduleCommand(
                                                BUSINESS_ID,
                                                RESOURCE_ID,
                                                DayOfWeek.MONDAY,
                                                windows)));

        assertEquals("Schedule windows must not overlap", exception.getMessage());
        verifyNoInteractions(resourceQueryPort, queryPort, commandPort);
    }

    @Test
    void missingResourceThrowsAndDoesNotSaveOrDelete() {
        when(resourceQueryPort.findByBusinessIdAndId(BUSINESS_ID, RESOURCE_ID))
                .thenReturn(Optional.empty());

        final var exception =
                assertThrows(
                        ResourceNotAvailableException.class,
                        () ->
                                service.replaceDateOverride(
                                        new ReplaceDateScheduleOverrideCommand(
                                                BUSINESS_ID,
                                                RESOURCE_ID,
                                                DATE,
                                                List.of(
                                                        new ScheduleWindow(
                                                                LocalTime.of(9, 0),
                                                                LocalTime.of(12, 0))))));

        assertEquals(
                "Resource is not available for business "
                        + BUSINESS_ID.value()
                        + ": "
                        + RESOURCE_ID.value(),
                exception.getMessage());
        verify(queryPort, never()).findDateOverride(any(), any(), any());
        verify(commandPort, never()).saveDateOverride(any());
        verify(commandPort, never()).saveWeekly(any());
        verify(commandPort, never()).deleteDateOverride(any(), any(), any());
    }

    @Test
    void missingResourceThrowsAndDoesNotDeleteOverride() {
        when(resourceQueryPort.findByBusinessIdAndId(BUSINESS_ID, RESOURCE_ID))
                .thenReturn(Optional.empty());

        final var exception =
                assertThrows(
                        ResourceNotAvailableException.class,
                        () -> service.deleteDateOverride(BUSINESS_ID, RESOURCE_ID, DATE));

        assertEquals(BUSINESS_ID, exception.businessId());
        assertEquals(RESOURCE_ID, exception.resourceId());
        verify(commandPort, never()).deleteDateOverride(any(), any(), any());
    }

    @Test
    void existingWeeklyReplacementPreservesCreatedAt() {
        final var existing =
                WeeklyResourceSchedule.create(
                        BUSINESS_ID,
                        RESOURCE_ID,
                        DayOfWeek.MONDAY,
                        List.of(new ScheduleWindow(LocalTime.of(8, 0), LocalTime.of(10, 0))),
                        CREATED_AT);
        final var replacement =
                List.of(new ScheduleWindow(LocalTime.of(10, 0), LocalTime.of(14, 0)));
        when(resourceQueryPort.findByBusinessIdAndId(BUSINESS_ID, RESOURCE_ID))
                .thenReturn(Optional.of(resource()));
        when(queryPort.findWeekly(BUSINESS_ID, RESOURCE_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(existing));

        service.replaceWeekly(
                new ReplaceWeeklyScheduleCommand(
                        BUSINESS_ID, RESOURCE_ID, DayOfWeek.MONDAY, replacement));

        final var captor = ArgumentCaptor.forClass(WeeklyResourceSchedule.class);
        verify(commandPort).saveWeekly(captor.capture());
        final var saved = captor.getValue();
        assertEquals(CREATED_AT, saved.createdAt());
        assertEquals(NOW, saved.updatedAt());
        assertEquals(replacement, saved.windows());
    }

    private static Resource resource() {
        return Resource.reconstitute(
                RESOURCE_ID,
                BUSINESS_ID,
                new ResourceName("Room A"),
                new ResourceSlug("room-a"),
                null,
                io.resrv.timeslot.domain.resource.ResourceStatus.ACTIVE,
                ResourceBookingOverrides.none(),
                CREATED_AT,
                CREATED_AT);
    }
}
