package io.resrv.application.availability.out;

import io.resrv.domain.availability.DateAvailabilityOverride;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.LocalDate;
import java.util.Optional;

public interface DateAvailabilityOverrideQueryPort {

    Optional<DateAvailabilityOverride> findByTenantIdAndResourceIdAndDate(
            TenantId tenantId, ResourceId resourceId, LocalDate date);
}
