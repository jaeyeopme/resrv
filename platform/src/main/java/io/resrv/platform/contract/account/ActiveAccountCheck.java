package io.resrv.platform.contract.account;

import io.resrv.shared.kernel.AccountId;

public interface ActiveAccountCheck {

    boolean isActive(AccountId accountId);
}
