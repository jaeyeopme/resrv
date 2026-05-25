package io.resrv.timeslot.application.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.application.business.BusinessNotAvailableException;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.settings.in.UpsertBusinessBookingSettingsCommand;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsCommandPort;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BusinessBookingSettingsServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2025-12-01T00:00:00Z");
    private static final BusinessId BUSINESS_ID = BusinessId.create();

    private BusinessLookupPort businessLookupPort;
    private BusinessBookingSettingsCommandPort commandPort;
    private BusinessBookingSettingsQueryPort queryPort;
    private BusinessBookingSettingsService service;

    @BeforeEach
    void setUp() {
        businessLookupPort = mock(BusinessLookupPort.class);
        commandPort = mock(BusinessBookingSettingsCommandPort.class);
        queryPort = mock(BusinessBookingSettingsQueryPort.class);
        service =
                new BusinessBookingSettingsService(
                        businessLookupPort,
                        commandPort,
                        queryPort,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsNewSettingsWhenBusinessActiveAndNoExistingSettings() {
        when(businessLookupPort.findActiveById(BUSINESS_ID))
                .thenReturn(Optional.of(activeBusiness()));
        when(queryPort.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        final var result =
                service.upsert(
                        new UpsertBusinessBookingSettingsCommand(BUSINESS_ID, 30, 10, 120, 90));

        assertEquals(BUSINESS_ID.value(), result.businessId());
        assertEquals(30, result.slotDurationMinutes());
        assertEquals(10, result.holdTtlMinutes());
        assertEquals(120, result.cancellationWindowMinutes());
        assertEquals(90, result.maxAdvanceBookingDays());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.updatedAt());

        final var captor = ArgumentCaptor.forClass(BusinessBookingSettings.class);
        verify(commandPort).save(captor.capture());
        final var saved = captor.getValue();
        assertEquals(BUSINESS_ID, saved.businessId());
        assertEquals(new SlotDuration(30), saved.slotDuration());
        assertEquals(new HoldTtl(10), saved.holdTtl());
        assertEquals(new CancellationWindow(120), saved.cancellationWindow());
        assertEquals(new MaxAdvanceBookingDays(90), saved.maxAdvanceBookingDays());
        assertEquals(NOW, saved.createdAt());
        assertEquals(NOW, saved.updatedAt());
    }

    @Test
    void updatesExistingSettingsPreservingCreatedAtAndChangingUpdatedAt() {
        final var existing =
                BusinessBookingSettings.reconstitute(
                        BUSINESS_ID,
                        new SlotDuration(30),
                        new HoldTtl(10),
                        new CancellationWindow(120),
                        new MaxAdvanceBookingDays(90),
                        CREATED_AT,
                        CREATED_AT);
        when(businessLookupPort.findActiveById(BUSINESS_ID))
                .thenReturn(Optional.of(activeBusiness()));
        when(queryPort.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(existing));

        final var result =
                service.upsert(
                        new UpsertBusinessBookingSettingsCommand(BUSINESS_ID, 60, 5, 240, 180));

        assertEquals(CREATED_AT, result.createdAt());
        assertEquals(NOW, result.updatedAt());
        assertEquals(60, result.slotDurationMinutes());
        assertEquals(5, result.holdTtlMinutes());
        assertEquals(240, result.cancellationWindowMinutes());
        assertEquals(180, result.maxAdvanceBookingDays());

        final var captor = ArgumentCaptor.forClass(BusinessBookingSettings.class);
        verify(commandPort).save(captor.capture());
        final var saved = captor.getValue();
        assertEquals(BUSINESS_ID, saved.businessId());
        assertEquals(CREATED_AT, saved.createdAt());
        assertEquals(NOW, saved.updatedAt());
    }

    @Test
    void absentBusinessThrowsAndDoesNotSave() {
        when(businessLookupPort.findActiveById(BUSINESS_ID)).thenReturn(Optional.empty());

        final var exception =
                assertThrows(
                        BusinessNotAvailableException.class,
                        () ->
                                service.upsert(
                                        new UpsertBusinessBookingSettingsCommand(
                                                BUSINESS_ID, 30, 10, 120, 90)));

        assertEquals("Business is not available: " + BUSINESS_ID.value(), exception.getMessage());
        verify(queryPort, never()).findByBusinessId(any());
        verify(commandPort, never()).save(any());
    }

    @Test
    void invalidPrimitiveSettingsFailBeforeSave() {
        when(businessLookupPort.findActiveById(BUSINESS_ID))
                .thenReturn(Optional.of(activeBusiness()));

        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.upsert(
                                        new UpsertBusinessBookingSettingsCommand(
                                                BUSINESS_ID, 7, 10, 120, 90)));

        assertEquals(
                "Slot duration must be 5-480 minutes in 5 minute increments",
                exception.getMessage());
        verify(queryPort, never()).findByBusinessId(any());
        verify(commandPort, never()).save(any());
    }

    private static BusinessLookupPort.BusinessView activeBusiness() {
        return new BusinessLookupPort.BusinessView(
                BUSINESS_ID, "Owner Studio", "owner-studio", Timezone.of("Asia/Seoul"));
    }
}
