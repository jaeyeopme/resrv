package io.resrv.ticketing.domain.purchase;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public record PurchaseConfirmationIdempotency(
        PurchaseConfirmationIdempotencyKey idempotencyKey,
        AccountId customerAccountId,
        TicketEventId ticketEventId,
        List<TicketSeatId> selectedSeatIds,
        String requestFingerprint,
        PurchaseConfirmationIdempotencyStatus status,
        TicketPurchaseId ticketPurchaseId,
        Instant createdAt,
        Instant completedAt,
        Instant expiresAt,
        Instant cleanupEligibleAt) {

    public static final Duration REPLAY_WINDOW = Duration.ofHours(24);
    public static final Duration EXPIRED_RETENTION = Duration.ofDays(30);

    public PurchaseConfirmationIdempotency {
        Objects.requireNonNull(idempotencyKey, "Idempotency key must not be null");
        Objects.requireNonNull(customerAccountId, "Customer account id must not be null");
        Objects.requireNonNull(ticketEventId, "Ticket event id must not be null");
        Objects.requireNonNull(selectedSeatIds, "Selected seat ids must not be null");
        selectedSeatIds = List.copyOf(selectedSeatIds);
        if (selectedSeatIds.isEmpty()) {
            throw new IllegalArgumentException("Selected seat ids must not be empty");
        }
        Objects.requireNonNull(requestFingerprint, "Request fingerprint must not be null");
        Objects.requireNonNull(status, "Idempotency status must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(expiresAt, "Expires at must not be null");
        Objects.requireNonNull(cleanupEligibleAt, "Cleanup eligible at must not be null");
        if (status == PurchaseConfirmationIdempotencyStatus.PURCHASED && ticketPurchaseId == null) {
            throw new IllegalArgumentException("Purchased idempotency records require purchase id");
        }
    }

    public static PurchaseConfirmationIdempotency pending(
            final PurchaseConfirmationIdempotencyKey idempotencyKey,
            final AccountId customerAccountId,
            final TicketEventId ticketEventId,
            final List<TicketSeatId> selectedSeatIds,
            final Instant now) {
        final var expiresAt = now.plus(REPLAY_WINDOW);
        return new PurchaseConfirmationIdempotency(
                idempotencyKey,
                customerAccountId,
                ticketEventId,
                selectedSeatIds,
                fingerprint(ticketEventId, selectedSeatIds),
                PurchaseConfirmationIdempotencyStatus.PENDING,
                null,
                now,
                null,
                expiresAt,
                expiresAt.plus(EXPIRED_RETENTION));
    }

    public PurchaseConfirmationIdempotency complete(
            final PurchaseConfirmationIdempotencyStatus status,
            final TicketPurchaseId ticketPurchaseId,
            final Instant completedAt) {
        if (status == PurchaseConfirmationIdempotencyStatus.PENDING) {
            throw new IllegalArgumentException("Completed idempotency status must not be pending");
        }
        return new PurchaseConfirmationIdempotency(
                idempotencyKey,
                customerAccountId,
                ticketEventId,
                selectedSeatIds,
                requestFingerprint,
                status,
                ticketPurchaseId,
                createdAt,
                completedAt,
                expiresAt,
                cleanupEligibleAt);
    }

    public boolean matches(final TicketEventId ticketEventId, final List<TicketSeatId> seatIds) {
        return requestFingerprint.equals(fingerprint(ticketEventId, seatIds));
    }

    public boolean expiredAt(final Instant now) {
        return !now.isBefore(expiresAt);
    }

    public static String fingerprint(
            final TicketEventId ticketEventId, final List<TicketSeatId> selectedSeatIds) {
        final var canonicalSeatIds =
                selectedSeatIds.stream().map(seatId -> seatId.value().toString()).sorted().toList();
        final var raw = ticketEventId.value() + ":" + String.join(",", canonicalSeatIds);
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
