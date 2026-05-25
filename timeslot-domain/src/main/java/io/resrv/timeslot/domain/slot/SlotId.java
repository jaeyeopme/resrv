package io.resrv.timeslot.domain.slot;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public record SlotId(String value) {

    public SlotId {
        Objects.requireNonNull(value, "Slot id must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Slot id must not be blank");
        }
    }

    public static SlotId of(
            final BusinessId businessId,
            final ResourceId resourceId,
            final Instant startAt,
            final Instant endAt) {
        final var raw = businessId.value() + "|" + resourceId.value() + "|" + startAt + "|" + endAt;
        return new SlotId(
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
    }

    public DecodedSlotId decode() {
        final var raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        final var parts = raw.split("\\|");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid slotId");
        }
        return new DecodedSlotId(
                UUID.fromString(parts[0]),
                UUID.fromString(parts[1]),
                Instant.parse(parts[2]),
                Instant.parse(parts[3]));
    }

    public record DecodedSlotId(UUID businessId, UUID resourceId, Instant startAt, Instant endAt) {}
}
