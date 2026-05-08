package io.resrv.domain.customer;

public final class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(final CustomerId customerId) {
        super("Customer '%s' was not found".formatted(customerId.value()));
    }
}
