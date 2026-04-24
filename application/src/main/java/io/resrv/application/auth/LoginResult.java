package io.resrv.application.auth;

public record LoginResult(String accessToken, long expiresIn) {}
