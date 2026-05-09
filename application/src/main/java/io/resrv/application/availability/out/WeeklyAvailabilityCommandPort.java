package io.resrv.application.availability.out;

import io.resrv.domain.availability.WeeklyAvailability;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.DayOfWeek;

public interface WeeklyAvailabilityCommandPort {

    void save(WeeklyAvailability availability);

    void deleteByTenantIdAndResourceIdAndDayOfWeek(
            TenantId tenantId, ResourceId resourceId, DayOfWeek dayOfWeek);
}
