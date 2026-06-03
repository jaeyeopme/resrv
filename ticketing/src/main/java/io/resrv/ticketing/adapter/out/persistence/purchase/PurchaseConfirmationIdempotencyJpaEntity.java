package io.resrv.ticketing.adapter.out.persistence.purchase;

import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "ticketing", name = "ticket_purchase_idempotency")
@IdClass(PurchaseConfirmationIdempotencyJpaEntity.Key.class)
class PurchaseConfirmationIdempotencyJpaEntity {

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Id
    @Column(name = "customer_account_id", nullable = false)
    private UUID customerAccountId;

    @Column(name = "ticket_event_id", nullable = false)
    private UUID ticketEventId;

    @Column(name = "selected_seat_ids", nullable = false)
    private String selectedSeatIds;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PurchaseConfirmationIdempotencyStatus status;

    @Column(name = "ticket_purchase_id")
    private UUID ticketPurchaseId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "cleanup_eligible_at", nullable = false)
    private Instant cleanupEligibleAt;

    protected PurchaseConfirmationIdempotencyJpaEntity() {}

    PurchaseConfirmationIdempotencyJpaEntity(
            final String idempotencyKey,
            final UUID customerAccountId,
            final UUID ticketEventId,
            final String selectedSeatIds,
            final String requestFingerprint,
            final PurchaseConfirmationIdempotencyStatus status,
            final UUID ticketPurchaseId,
            final Instant createdAt,
            final Instant completedAt,
            final Instant expiresAt,
            final Instant cleanupEligibleAt) {
        this.idempotencyKey = idempotencyKey;
        this.customerAccountId = customerAccountId;
        this.ticketEventId = ticketEventId;
        this.selectedSeatIds = selectedSeatIds;
        this.requestFingerprint = requestFingerprint;
        this.status = status;
        this.ticketPurchaseId = ticketPurchaseId;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.expiresAt = expiresAt;
        this.cleanupEligibleAt = cleanupEligibleAt;
    }

    String idempotencyKey() {
        return idempotencyKey;
    }

    UUID customerAccountId() {
        return customerAccountId;
    }

    UUID ticketEventId() {
        return ticketEventId;
    }

    String selectedSeatIds() {
        return selectedSeatIds;
    }

    String requestFingerprint() {
        return requestFingerprint;
    }

    PurchaseConfirmationIdempotencyStatus status() {
        return status;
    }

    UUID ticketPurchaseId() {
        return ticketPurchaseId;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant completedAt() {
        return completedAt;
    }

    Instant expiresAt() {
        return expiresAt;
    }

    Instant cleanupEligibleAt() {
        return cleanupEligibleAt;
    }

    record Key(UUID customerAccountId, String idempotencyKey) implements Serializable {}
}
