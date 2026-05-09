package io.resrv.application.customer.in;

public record RegisterCustomerCommand(
        String tenantSlug, String email, String name, String password) {}
