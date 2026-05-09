package io.resrv.application.customer.in;

public record CustomerLoginCommand(String tenantSlug, String email, String password) {}
