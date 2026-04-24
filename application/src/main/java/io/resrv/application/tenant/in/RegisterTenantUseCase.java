package io.resrv.application.tenant.in;

import io.resrv.domain.tenant.Tenant;

public interface RegisterTenantUseCase {

    Tenant register(final RegisterTenantCommand command);
}
