package io.resrv.platform.contract.membership;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;

public interface BusinessAccessCheck {

    /**
     * Returns true only when the account is active, the business is active, and the account has an
     * active owner/staff membership. Implementations may record non-sensitive denial facts for
     * operator investigation, but must not expose account existence or credential details.
     */
    boolean hasBusinessAccess(AccountId accountId, BusinessId businessId);
}
