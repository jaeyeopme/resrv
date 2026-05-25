package io.resrv.platform.application.auth.in;

public record LoginResult(String accessToken, long expiresIn) {}
