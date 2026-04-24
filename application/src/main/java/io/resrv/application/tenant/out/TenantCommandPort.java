package io.resrv.application.tenant.out;

import io.resrv.domain.tenant.Tenant;

public interface TenantCommandPort {

    void save(final Tenant tenant);
}
