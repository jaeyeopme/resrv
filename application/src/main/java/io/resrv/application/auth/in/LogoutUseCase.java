package io.resrv.application.auth.in;

public interface LogoutUseCase {

    void logout(final LogoutCommand command);
}
