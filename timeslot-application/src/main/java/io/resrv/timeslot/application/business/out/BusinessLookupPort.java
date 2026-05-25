package io.resrv.timeslot.application.business.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.Optional;

public interface BusinessLookupPort {

    Optional<BusinessView> findActiveById(BusinessId businessId);

    record BusinessView(BusinessId id, String name, String slug, Timezone timezone) {}
}
