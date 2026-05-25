package io.resrv.timeslot.domain.slot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

final class SlotTest {

    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final Instant START = Instant.parse("2026-05-25T00:00:00Z");
    private static final Instant END = Instant.parse("2026-05-25T00:30:00Z");

    @Test
    void slotIdIsDeterministicAndDecodable() {
        final var slotId = SlotId.of(BUSINESS_ID, RESOURCE_ID, START, END);
        final var sameSlotId = SlotId.of(BUSINESS_ID, RESOURCE_ID, START, END);

        assertEquals(slotId, sameSlotId);

        final var decoded = slotId.decode();
        assertEquals(BUSINESS_ID.value(), decoded.businessId());
        assertEquals(RESOURCE_ID.value(), decoded.resourceId());
        assertEquals(START, decoded.startAt());
        assertEquals(END, decoded.endAt());
    }

    @Test
    void slotIdRejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> new SlotId(null));
        assertThrows(IllegalArgumentException.class, () -> new SlotId(" "));
        assertThrows(IllegalArgumentException.class, () -> new SlotId("bad").decode());
    }

    @Test
    void slotRequiresStartBeforeEnd() {
        final var slotId = SlotId.of(BUSINESS_ID, RESOURCE_ID, START, END);

        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new Slot(
                                        slotId,
                                        BUSINESS_ID,
                                        RESOURCE_ID,
                                        END,
                                        START,
                                        OffsetDateTime.ofInstant(END, ZoneOffset.UTC),
                                        OffsetDateTime.ofInstant(START, ZoneOffset.UTC)));

        assertEquals("Slot start must be before end", exception.getMessage());
    }

    @Test
    void slotRejectsNullFields() {
        final var slotId = SlotId.of(BUSINESS_ID, RESOURCE_ID, START, END);

        assertThrows(
                NullPointerException.class,
                () ->
                        new Slot(
                                null,
                                BUSINESS_ID,
                                RESOURCE_ID,
                                START,
                                END,
                                OffsetDateTime.ofInstant(START, ZoneOffset.UTC),
                                OffsetDateTime.ofInstant(END, ZoneOffset.UTC)));
        assertThrows(
                NullPointerException.class,
                () ->
                        new Slot(
                                slotId,
                                BUSINESS_ID,
                                RESOURCE_ID,
                                START,
                                END,
                                null,
                                OffsetDateTime.ofInstant(END, ZoneOffset.UTC)));
    }
}
