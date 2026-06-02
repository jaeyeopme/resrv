package io.resrv.ticketing.adapter.out.platform;

import io.resrv.platform.exchange.business.ActiveBusinessLookup;
import io.resrv.platform.exchange.business.ActiveBusinessView;
import io.resrv.platform.exchange.membership.BusinessAccessCheck;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.ticketing.application.platform.out.TicketingBusinessAccessPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class PlatformExchangeTicketingBusinessAccessAdapter implements TicketingBusinessAccessPort {

    private final ActiveBusinessLookup activeBusinessLookup;
    private final BusinessAccessCheck businessAccessCheck;

    PlatformExchangeTicketingBusinessAccessAdapter(
            final ActiveBusinessLookup activeBusinessLookup,
            final BusinessAccessCheck businessAccessCheck) {
        this.activeBusinessLookup = activeBusinessLookup;
        this.businessAccessCheck = businessAccessCheck;
    }

    @Override
    public Optional<BusinessView> findActiveBusiness(final BusinessId businessId) {
        return activeBusinessLookup.findActiveById(businessId).map(this::toView);
    }

    @Override
    public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
        return businessAccessCheck.hasBusinessAccess(accountId, businessId);
    }

    private BusinessView toView(final ActiveBusinessView business) {
        return new BusinessView(business.id(), business.name(), business.timezone());
    }
}
