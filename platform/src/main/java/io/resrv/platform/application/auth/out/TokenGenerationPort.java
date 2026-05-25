package io.resrv.platform.application.auth.out;

import io.resrv.platform.application.auth.in.LoginResult;
import io.resrv.shared.kernel.AccountId;

public interface TokenGenerationPort {

    LoginResult generate(AccountId accountId);
}
