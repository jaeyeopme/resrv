package io.resrv.application.auth.in;

public record LoginCommand(String tenantSlug, String email, String password) {}
