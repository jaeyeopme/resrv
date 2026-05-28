package io.resrv.platform.contract.business;

import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;

/**
 * Platform-owned current display lookup for historical references.
 *
 * <p>This contract may return inactive businesses because customer reservation history still needs
 * to render current business summary data for reservations the customer already owns. Do not use it
 * for authorization, booking availability, or business-scoped write decisions.
 */
public interface BusinessSummaryLookup {

    Optional<BusinessSummaryView> findCurrentSummaryById(BusinessId businessId);
}
