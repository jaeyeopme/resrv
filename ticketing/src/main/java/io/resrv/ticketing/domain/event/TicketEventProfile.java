package io.resrv.ticketing.domain.event;

import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import java.util.Objects;

public record TicketEventProfile(
        String title, Instant eventStartAt, Instant eventEndAt, Timezone timezone) {

    private static final int MAX_TITLE_LENGTH = 200;

    public TicketEventProfile {
        title = normalizeTitle(title);
        Objects.requireNonNull(eventStartAt, "Event start time must not be null");
        Objects.requireNonNull(eventEndAt, "Event end time must not be null");
        Objects.requireNonNull(timezone, "Event timezone must not be null");
        if (!eventStartAt.isBefore(eventEndAt)) {
            throw new IllegalArgumentException("Event start time must be before event end time");
        }
    }

    private static String normalizeTitle(final String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Ticket event title must not be blank");
        }
        final var trimmed = title.strip();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Ticket event title must be 1-200 characters");
        }
        return trimmed;
    }
}
