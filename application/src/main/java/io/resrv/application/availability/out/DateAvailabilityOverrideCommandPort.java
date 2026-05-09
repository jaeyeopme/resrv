package io.resrv.application.availability.out;

import io.resrv.domain.availability.DateAvailabilityOverride;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.LocalDate;

public interface DateAvailabilityOverrideCommandPort {

    void save(DateAvailabilityOverride override);

    void deleteByTenantIdAndResourceIdAndDate(
            TenantId tenantId, ResourceId resourceId, LocalDate date);
}
