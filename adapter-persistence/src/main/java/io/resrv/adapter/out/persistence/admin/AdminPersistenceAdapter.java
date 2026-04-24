package io.resrv.adapter.out.persistence.admin;

import io.resrv.application.admin.out.AdminCommandPort;
import io.resrv.application.admin.out.AdminQueryPort;
import io.resrv.application.auth.out.UserCredentials;
import io.resrv.domain.admin.Admin;
import io.resrv.domain.tenant.TenantId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class AdminPersistenceAdapter implements AdminCommandPort, AdminQueryPort {

    private final AdminJpaRepository adminJpaRepository;

    AdminPersistenceAdapter(final AdminJpaRepository adminJpaRepository) {
        this.adminJpaRepository = adminJpaRepository;
    }

    @Override
    public void save(final Admin admin) {
        adminJpaRepository.save(AdminPersistenceMapper.toJpaEntity(admin));
    }

    @Override
    public Optional<UserCredentials> findCredentialsByTenantIdAndEmail(
            final TenantId tenantId, final String email) {
        return adminJpaRepository
                .findByTenantIdAndEmail(tenantId.value(), email)
                .map(
                        entity ->
                                new UserCredentials(
                                        entity.getId(),
                                        entity.getTenantId(),
                                        entity.getHashedPassword(),
                                        entity.getRole(),
                                        entity.isActive()));
    }
}
