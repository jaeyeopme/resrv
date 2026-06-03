package io.resrv.ticketing.application.purchase.out;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotency;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyKey;
import java.util.Optional;

public interface PurchaseConfirmationIdempotencyPort {

    Optional<PurchaseConfirmationIdempotency> findForCustomerKey(
            AccountId customerAccountId, PurchaseConfirmationIdempotencyKey idempotencyKey);

    PurchaseConfirmationIdempotency createPendingOrFindExisting(
            PurchaseConfirmationIdempotency pendingIdempotency);

    PurchaseConfirmationIdempotency save(PurchaseConfirmationIdempotency idempotency);
}
