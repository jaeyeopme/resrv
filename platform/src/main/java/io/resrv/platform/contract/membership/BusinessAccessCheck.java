package io.resrv.platform.contract.membership;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;

public interface BusinessAccessCheck {

    boolean hasBusinessAccess(AccountId accountId, BusinessId businessId);
}
