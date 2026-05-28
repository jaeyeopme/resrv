package io.resrv.platform.contract.membership;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;

/**
 * Platform-owned authorization check for business-scoped owner/staff actions.
 *
 * <p>This is a decision contract, not a data lookup. Consumers must not infer partial facts such as
 * whether the account, business, or membership exists from a false result.
 */
public interface BusinessAccessCheck {

    /**
     * Returns true only when the account is active, the business is active, and the account has an
     * active owner/staff membership. Implementations may record non-sensitive denial facts for
     * operator investigation, but must not expose account existence or credential details.
     */
    boolean hasBusinessAccess(AccountId accountId, BusinessId businessId);
}
