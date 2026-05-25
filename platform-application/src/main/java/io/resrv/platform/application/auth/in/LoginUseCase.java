package io.resrv.platform.application.auth.in;

public interface LoginUseCase {

    LoginResult login(LoginCommand command);
}
