package io.resrv.ticketing.application.platform.out;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.Objects;
import java.util.Optional;

public interface TicketingBusinessAccessPort {

    Optional<BusinessView> findActiveBusiness(BusinessId businessId);

    boolean hasBusinessAccess(AccountId accountId, BusinessId businessId);

    record BusinessView(BusinessId id, String name, Timezone timezone) {

        public BusinessView {
            Objects.requireNonNull(id, "Business id must not be null");
            Objects.requireNonNull(name, "Business name must not be null");
            Objects.requireNonNull(timezone, "Business timezone must not be null");
        }
    }
}
