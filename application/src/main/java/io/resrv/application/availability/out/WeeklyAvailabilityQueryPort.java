package io.resrv.application.availability.out;

import io.resrv.domain.availability.WeeklyAvailability;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.DayOfWeek;
import java.util.Optional;

public interface WeeklyAvailabilityQueryPort {

    Optional<WeeklyAvailability> findByTenantIdAndResourceIdAndDayOfWeek(
            TenantId tenantId, ResourceId resourceId, DayOfWeek dayOfWeek);
}
