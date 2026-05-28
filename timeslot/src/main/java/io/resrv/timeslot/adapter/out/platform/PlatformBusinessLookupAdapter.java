package io.resrv.timeslot.adapter.out.platform;

import io.resrv.platform.contract.business.ActiveBusinessLookup;
import io.resrv.platform.contract.business.BusinessSummaryLookup;
import io.resrv.platform.contract.membership.BusinessAccessCheck;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class PlatformBusinessLookupAdapter implements BusinessLookupPort, BusinessAccessPort {

    private final ActiveBusinessLookup activeBusinessLookup;
    private final BusinessSummaryLookup businessSummaryLookup;
    private final BusinessAccessCheck businessAccessCheck;

    PlatformBusinessLookupAdapter(
            final ActiveBusinessLookup activeBusinessLookup,
            final BusinessSummaryLookup businessSummaryLookup,
            final BusinessAccessCheck businessAccessCheck) {
        this.activeBusinessLookup = activeBusinessLookup;
        this.businessSummaryLookup = businessSummaryLookup;
        this.businessAccessCheck = businessAccessCheck;
    }

    @Override
    public Optional<BusinessView> findActiveById(final BusinessId businessId) {
        return activeBusinessLookup
                .findActiveById(businessId)
                .map(
                        business ->
                                new BusinessView(
                                        business.id(),
                                        business.name(),
                                        business.slug(),
                                        business.timezone()));
    }

    @Override
    public Optional<BusinessView> findCurrentSummaryById(final BusinessId businessId) {
        return businessSummaryLookup
                .findCurrentSummaryById(businessId)
                .map(
                        business ->
                                new BusinessView(
                                        business.id(),
                                        business.name(),
                                        business.slug(),
                                        business.timezone()));
    }

    @Override
    public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
        return businessAccessCheck.hasBusinessAccess(accountId, businessId);
    }
}
