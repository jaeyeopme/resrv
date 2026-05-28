package io.resrv.platform.contract.business;

import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;

/**
 * Platform-owned lookup for use cases that require a currently active business.
 *
 * <p>This contract intentionally hides inactive businesses. Timeslot uses it for booking,
 * scheduling, settings, and business-scoped flows where inactive businesses must behave as
 * unavailable.
 */
public interface ActiveBusinessLookup {

    Optional<ActiveBusinessView> findActiveById(BusinessId businessId);

    Optional<ActiveBusinessView> findActiveBySlug(String slug);
}
