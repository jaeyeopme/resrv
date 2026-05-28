package io.resrv.timeslot.application.business.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.Objects;
import java.util.Optional;

/**
 * Timeslot port for platform-owned business display and availability data.
 *
 * <p>{@link #findActiveById(BusinessId)} is for flows where inactive businesses are unavailable.
 * {@link #findCurrentSummaryById(BusinessId)} is for historical customer-owned reservation
 * rendering and may return inactive businesses.
 */
public interface BusinessLookupPort {

    Optional<BusinessView> findActiveById(BusinessId businessId);

    Optional<BusinessView> findActiveBySlug(String slug);

    Optional<BusinessView> findCurrentSummaryById(BusinessId businessId);

    Optional<BusinessView> findCurrentSummaryBySlug(String slug);

    record BusinessView(BusinessId id, String name, String slug, Timezone timezone) {

        public BusinessView {
            Objects.requireNonNull(id, "Business id must not be null");
            Objects.requireNonNull(name, "Business name must not be null");
            Objects.requireNonNull(slug, "Business slug must not be null");
            Objects.requireNonNull(timezone, "Business timezone must not be null");
        }
    }
}
