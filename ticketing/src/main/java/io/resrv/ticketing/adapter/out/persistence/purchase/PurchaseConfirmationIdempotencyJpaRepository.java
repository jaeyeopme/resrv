package io.resrv.ticketing.adapter.out.persistence.purchase;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface PurchaseConfirmationIdempotencyJpaRepository
        extends JpaRepository<
                PurchaseConfirmationIdempotencyJpaEntity,
                PurchaseConfirmationIdempotencyJpaEntity.Key> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseConfirmationIdempotencyJpaEntity> findByCustomerAccountIdAndIdempotencyKey(
            UUID customerAccountId, String idempotencyKey);
}
