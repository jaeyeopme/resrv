package io.resrv.application.auth.out;

public record TokenResult(String accessToken, long expiresIn) {}
