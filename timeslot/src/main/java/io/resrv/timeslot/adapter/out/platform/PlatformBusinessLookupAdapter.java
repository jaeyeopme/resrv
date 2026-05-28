package io.resrv.timeslot.adapter.out.platform;

import io.resrv.platform.contract.business.ActiveBusinessLookup;
import io.resrv.platform.contract.business.ActiveBusinessView;
import io.resrv.platform.contract.business.BusinessSummaryLookup;
import io.resrv.platform.contract.business.BusinessSummaryView;
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
                .map(PlatformBusinessLookupAdapter::toView);
    }

    @Override
    public Optional<BusinessView> findActiveBySlug(final String slug) {
        return activeBusinessLookup
                .findActiveBySlug(slug)
                .map(PlatformBusinessLookupAdapter::toView);
    }

    @Override
    public Optional<BusinessView> findCurrentSummaryById(final BusinessId businessId) {
        return businessSummaryLookup
                .findCurrentSummaryById(businessId)
                .map(PlatformBusinessLookupAdapter::toView);
    }

    @Override
    public Optional<BusinessView> findCurrentSummaryBySlug(final String slug) {
        return businessSummaryLookup
                .findCurrentSummaryBySlug(slug)
                .map(PlatformBusinessLookupAdapter::toView);
    }

    @Override
    public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
        return businessAccessCheck.hasBusinessAccess(accountId, businessId);
    }

    private static BusinessView toView(final ActiveBusinessView business) {
        return new BusinessView(
                business.id(), business.name(), business.slug(), business.timezone());
    }

    private static BusinessView toView(final BusinessSummaryView business) {
        return new BusinessView(
                business.id(), business.name(), business.slug(), business.timezone());
    }
}
