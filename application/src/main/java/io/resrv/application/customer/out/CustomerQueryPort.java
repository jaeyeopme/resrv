package io.resrv.application.customer.out;

import io.resrv.application.auth.out.UserCredentials;
import io.resrv.domain.customer.Customer;
import io.resrv.domain.customer.CustomerEmail;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.tenant.TenantId;
import java.util.Optional;

public interface CustomerQueryPort {

    boolean existsByTenantIdAndEmail(TenantId tenantId, CustomerEmail email);

    Optional<UserCredentials> findCredentialsByTenantIdAndEmail(TenantId tenantId, String email);

    Optional<Customer> findByTenantIdAndId(TenantId tenantId, CustomerId customerId);
}
