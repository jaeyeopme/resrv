package io.resrv.timeslot.application.business.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.Objects;
import java.util.Optional;

public interface BusinessLookupPort {

    Optional<BusinessView> findActiveById(BusinessId businessId);

    record BusinessView(BusinessId id, String name, String slug, Timezone timezone) {

        public BusinessView {
            Objects.requireNonNull(id, "Business id must not be null");
            Objects.requireNonNull(name, "Business name must not be null");
            Objects.requireNonNull(slug, "Business slug must not be null");
            Objects.requireNonNull(timezone, "Business timezone must not be null");
        }
    }
}
