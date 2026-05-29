package io.resrv.platform.exchange.account;

import io.resrv.shared.kernel.AccountId;

public interface ActiveAccountCheck {

    boolean isActive(AccountId accountId);
}
