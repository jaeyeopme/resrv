package io.resrv.timeslot.adapter.out.platform;

import io.resrv.platform.application.business.in.LookupActiveBusinessUseCase;
import io.resrv.platform.application.membership.in.CheckBusinessAccessUseCase;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class PlatformBusinessLookupAdapter implements BusinessLookupPort, BusinessAccessPort {

    private final LookupActiveBusinessUseCase lookupActiveBusinessUseCase;
    private final CheckBusinessAccessUseCase checkBusinessAccessUseCase;

    PlatformBusinessLookupAdapter(
            final LookupActiveBusinessUseCase lookupActiveBusinessUseCase,
            final CheckBusinessAccessUseCase checkBusinessAccessUseCase) {
        this.lookupActiveBusinessUseCase = lookupActiveBusinessUseCase;
        this.checkBusinessAccessUseCase = checkBusinessAccessUseCase;
    }

    @Override
    public Optional<BusinessView> findActiveById(final BusinessId businessId) {
        return lookupActiveBusinessUseCase
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
    public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
        return checkBusinessAccessUseCase.hasBusinessAccess(accountId, businessId);
    }
}
