package io.resrv.adapter.out.persistence.customer;

import io.resrv.domain.customer.Customer;
import io.resrv.domain.customer.CustomerEmail;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.customer.CustomerName;
import io.resrv.domain.tenant.TenantId;

final class CustomerPersistenceMapper {

    private CustomerPersistenceMapper() {}

    static CustomerJpaEntity toJpaEntity(final Customer customer) {
        return new CustomerJpaEntity(
                customer.id().value(),
                customer.tenantId().value(),
                customer.email().value(),
                customer.name().value(),
                customer.hashedPassword(),
                customer.active(),
                customer.createdAt());
    }

    static Customer toDomain(final CustomerJpaEntity entity) {
        return Customer.reconstitute(
                CustomerId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                new CustomerEmail(entity.getEmail()),
                new CustomerName(entity.getName()),
                entity.getHashedPassword(),
                entity.isActive(),
                entity.getCreatedAt());
    }
}
