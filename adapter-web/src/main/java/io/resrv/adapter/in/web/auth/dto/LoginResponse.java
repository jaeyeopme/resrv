package io.resrv.adapter.in.web.auth.dto;

import io.resrv.application.auth.LoginResult;

public record LoginResponse(String accessToken, long expiresIn, String tokenType) {

    public static LoginResponse from(final LoginResult result) {
        return new LoginResponse(result.accessToken(), result.expiresIn(), "Bearer");
    }
}
