package io.resrv.domain.customer;

import io.resrv.domain.tenant.TenantId;

public final class CustomerEmailAlreadyExistsException extends RuntimeException {

    public CustomerEmailAlreadyExistsException(
            final TenantId tenantId, final CustomerEmail customerEmail) {
        super(
                "Customer email '%s' is already in use for tenant '%s'"
                        .formatted(customerEmail.value(), tenantId.value()));
    }
}
