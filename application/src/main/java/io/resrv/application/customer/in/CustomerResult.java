package io.resrv.application.customer.in;

import io.resrv.domain.customer.Customer;
import java.time.Instant;
import java.util.UUID;

public record CustomerResult(
        UUID id, UUID tenantId, String email, String name, boolean active, Instant createdAt) {

    public static CustomerResult from(final Customer customer) {
        return new CustomerResult(
                customer.id().value(),
                customer.tenantId().value(),
                customer.email().value(),
                customer.name().value(),
                customer.active(),
                customer.createdAt());
    }
}
