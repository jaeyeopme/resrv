package io.resrv.adapter.out.persistence.admin;

import io.resrv.domain.admin.Admin;

final class AdminPersistenceMapper {

    private AdminPersistenceMapper() {}

    static AdminJpaEntity toJpaEntity(final Admin admin) {
        return new AdminJpaEntity(
                admin.id().value(),
                admin.tenantId().value(),
                admin.email().value(),
                admin.hashedPassword(),
                admin.role().name(),
                admin.active(),
                admin.createdAt());
    }
}
