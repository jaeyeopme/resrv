package io.resrv.ticketing.adapter.out.persistence.purchase;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotency;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyKey;
import io.resrv.ticketing.domain.purchase.TicketPurchaseId;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.util.Arrays;
import java.util.UUID;

final class PurchaseConfirmationIdempotencyMapper {

    private PurchaseConfirmationIdempotencyMapper() {}

    static PurchaseConfirmationIdempotencyJpaEntity toEntity(
            final PurchaseConfirmationIdempotency idempotency) {
        return new PurchaseConfirmationIdempotencyJpaEntity(
                idempotency.idempotencyKey().value(),
                idempotency.customerAccountId().value(),
                idempotency.ticketEventId().value(),
                idempotency.selectedSeatIds().stream()
                        .map(seatId -> seatId.value().toString())
                        .sorted()
                        .reduce((left, right) -> left + "," + right)
                        .orElseThrow(),
                idempotency.requestFingerprint(),
                idempotency.status(),
                idempotency.ticketPurchaseId() == null
                        ? null
                        : idempotency.ticketPurchaseId().value(),
                idempotency.createdAt(),
                idempotency.completedAt(),
                idempotency.expiresAt(),
                idempotency.cleanupEligibleAt());
    }

    static PurchaseConfirmationIdempotency toDomain(
            final PurchaseConfirmationIdempotencyJpaEntity entity) {
        return new PurchaseConfirmationIdempotency(
                PurchaseConfirmationIdempotencyKey.of(entity.idempotencyKey()),
                AccountId.of(entity.customerAccountId()),
                TicketEventId.of(entity.ticketEventId()),
                Arrays.stream(entity.selectedSeatIds().split(","))
                        .map(UUID::fromString)
                        .map(TicketSeatId::of)
                        .toList(),
                entity.requestFingerprint(),
                entity.status(),
                entity.ticketPurchaseId() == null
                        ? null
                        : TicketPurchaseId.of(entity.ticketPurchaseId()),
                entity.createdAt(),
                entity.completedAt(),
                entity.expiresAt(),
                entity.cleanupEligibleAt());
    }
}
