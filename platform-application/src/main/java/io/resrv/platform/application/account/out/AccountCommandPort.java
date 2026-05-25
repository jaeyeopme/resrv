package io.resrv.platform.application.account.out;

import io.resrv.platform.domain.account.Account;

public interface AccountCommandPort {

    void save(Account account);
}
