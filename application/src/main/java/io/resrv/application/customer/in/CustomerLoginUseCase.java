package io.resrv.application.customer.in;

import io.resrv.application.auth.LoginResult;

public interface CustomerLoginUseCase {

    LoginResult login(CustomerLoginCommand command);
}
