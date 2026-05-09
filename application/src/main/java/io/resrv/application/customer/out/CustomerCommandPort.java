package io.resrv.application.customer.out;

import io.resrv.domain.customer.Customer;

public interface CustomerCommandPort {

    void save(Customer customer);
}
