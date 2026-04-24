package io.resrv.application.admin.out;

import io.resrv.application.auth.out.UserCredentials;
import io.resrv.domain.tenant.TenantId;
import java.util.Optional;

public interface AdminQueryPort {

    Optional<UserCredentials> findCredentialsByTenantIdAndEmail(
            final TenantId tenantId, final String email);
}
