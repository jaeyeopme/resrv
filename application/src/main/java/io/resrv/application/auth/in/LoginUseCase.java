package io.resrv.application.auth.in;

import io.resrv.application.auth.LoginResult;

public interface LoginUseCase {

    LoginResult login(final LoginCommand command);
}
