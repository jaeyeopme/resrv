package io.resrv.platform.application.auth.in;

public interface ResetPasswordUseCase {

    ResetPasswordResult resetPassword(ResetPasswordCommand command);
}
