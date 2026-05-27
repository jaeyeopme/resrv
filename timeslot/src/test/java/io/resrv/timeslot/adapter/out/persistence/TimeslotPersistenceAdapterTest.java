package io.resrv.timeslot.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.application.resource.out.ResourceCommandPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleCommandPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsCommandPort;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceBookingOverrides;
import io.resrv.timeslot.domain.resource.ResourceName;
import io.resrv.timeslot.domain.resource.ResourceSlug;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan("io.resrv.timeslot.adapter.out.persistence")
class TimeslotPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private BusinessBookingSettingsCommandPort settingsCommandPort;

    @Autowired private BusinessBookingSettingsQueryPort settingsQueryPort;

    @Autowired private ResourceCommandPort resourceCommandPort;

    @Autowired private ResourceQueryPort resourceQueryPort;

    @Autowired private ResourceScheduleCommandPort scheduleCommandPort;

    @Autowired private ResourceScheduleQueryPort scheduleQueryPort;

    @Test
    void savesAndLoadsSettingsResourceWeeklyScheduleAndDateOverride() {
        final var businessId = BusinessId.create();
        final var settings =
                BusinessBookingSettings.create(
                        businessId,
                        new SlotDuration(30),
                        new HoldTtl(10),
                        new CancellationWindow(60),
                        new MaxAdvanceBookingDays(30),
                        NOW);
        settingsCommandPort.save(settings);

        final var resource =
                Resource.create(
                        businessId,
                        new ResourceName("Room A"),
                        new ResourceSlug("room-a"),
                        "Room description",
                        new ResourceBookingOverrides(
                                new SlotDuration(60), new HoldTtl(5), new CancellationWindow(120)),
                        NOW);
        resourceCommandPort.save(resource);

        final var weekly =
                WeeklyResourceSchedule.create(
                        businessId,
                        resource.id(),
                        DayOfWeek.MONDAY,
                        List.of(
                                new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                                new ScheduleWindow(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                        NOW);
        scheduleCommandPort.saveWeekly(weekly);

        final var date = LocalDate.parse("2026-05-26");
        final var dateOverride =
                DateResourceScheduleOverride.create(
                        businessId, resource.id(), date, List.of(), NOW);
        scheduleCommandPort.saveDateOverride(dateOverride);

        final var foundSettings = settingsQueryPort.findByBusinessId(businessId).orElseThrow();
        assertEquals(30, foundSettings.slotDuration().minutes());
        assertEquals(10, foundSettings.holdTtl().minutes());

        final var foundResource =
                resourceQueryPort
                        .findByBusinessIdAndSlug(businessId, new ResourceSlug("room-a"))
                        .orElseThrow();
        assertEquals(resource.id(), foundResource.id());
        assertEquals(businessId, foundResource.businessId());
        assertEquals(60, foundResource.bookingOverrides().slotDuration().minutes());

        assertTrue(resourceQueryPort.findByBusinessIdAndId(businessId, resource.id()).isPresent());

        final var foundWeekly =
                scheduleQueryPort
                        .findWeekly(businessId, resource.id(), DayOfWeek.MONDAY)
                        .orElseThrow();
        assertEquals(businessId, foundWeekly.businessId());
        assertEquals(resource.id(), foundWeekly.resourceId());
        assertEquals(2, foundWeekly.windows().size());
        assertEquals(LocalTime.of(9, 0), foundWeekly.windows().get(0).startTime());

        final var foundOverride =
                scheduleQueryPort.findDateOverride(businessId, resource.id(), date).orElseThrow();
        assertEquals(businessId, foundOverride.businessId());
        assertEquals(resource.id(), foundOverride.resourceId());
        assertEquals(date, foundOverride.date());
        assertEquals(List.of(), foundOverride.windows());
    }

    @Test
    void replacesSchedulesAndDeletesDateOverride() {
        final var businessId = BusinessId.create();
        final var resource =
                Resource.create(
                        businessId,
                        new ResourceName("Room B"),
                        new ResourceSlug("room-b"),
                        null,
                        ResourceBookingOverrides.none(),
                        NOW);
        resourceCommandPort.save(resource);

        final var createdWeekly =
                WeeklyResourceSchedule.create(
                        businessId,
                        resource.id(),
                        DayOfWeek.TUESDAY,
                        List.of(
                                new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                                new ScheduleWindow(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                        NOW);
        scheduleCommandPort.saveWeekly(createdWeekly);

        final var updatedWeekly =
                createdWeekly.replaceWindows(
                        List.of(new ScheduleWindow(LocalTime.of(10, 0), LocalTime.of(14, 0))),
                        NOW.plusSeconds(60));
        scheduleCommandPort.saveWeekly(updatedWeekly);

        final var foundWeekly =
                scheduleQueryPort
                        .findWeekly(businessId, resource.id(), DayOfWeek.TUESDAY)
                        .orElseThrow();
        assertEquals(NOW, foundWeekly.createdAt());
        assertEquals(NOW.plusSeconds(60), foundWeekly.updatedAt());
        assertEquals(1, foundWeekly.windows().size());
        assertEquals(LocalTime.of(10, 0), foundWeekly.windows().getFirst().startTime());

        final var date = LocalDate.parse("2026-05-27");
        final var dateOverride =
                DateResourceScheduleOverride.create(
                        businessId,
                        resource.id(),
                        date,
                        List.of(new ScheduleWindow(LocalTime.of(15, 0), LocalTime.of(18, 0))),
                        NOW);
        scheduleCommandPort.saveDateOverride(dateOverride);
        assertTrue(scheduleQueryPort.findDateOverride(businessId, resource.id(), date).isPresent());

        scheduleCommandPort.deleteDateOverride(businessId, resource.id(), date);

        assertTrue(scheduleQueryPort.findDateOverride(businessId, resource.id(), date).isEmpty());
    }

    @Test
    void replacesResourceDetailsAndActiveQueriesExcludeInactiveResources() {
        final var businessId = BusinessId.create();
        final var resource =
                Resource.create(
                        businessId,
                        new ResourceName("Room C"),
                        new ResourceSlug("room-c"),
                        "Original",
                        ResourceBookingOverrides.none(),
                        NOW);
        resourceCommandPort.save(resource);

        final var replaced =
                resource.replaceDetails(
                        new ResourceName("Room C Updated"),
                        new ResourceSlug("room-c-updated"),
                        "Updated",
                        new ResourceBookingOverrides(
                                new SlotDuration(45), new HoldTtl(5), new CancellationWindow(180)),
                        NOW.plusSeconds(60));
        resourceCommandPort.save(replaced);

        final var found =
                resourceQueryPort.findByBusinessIdAndId(businessId, resource.id()).orElseThrow();
        assertEquals(resource.id(), found.id());
        assertEquals("Room C Updated", found.name().value());
        assertEquals("room-c-updated", found.slug().value());
        assertEquals(45, found.bookingOverrides().slotDuration().minutes());
        assertEquals(ResourceStatus.ACTIVE, found.status());
        assertEquals(1, resourceQueryPort.findActiveByBusinessId(businessId).size());

        resourceCommandPort.save(found.deactivate(NOW.plusSeconds(120)));

        assertTrue(resourceQueryPort.findActiveByBusinessId(businessId).isEmpty());
    }
}
