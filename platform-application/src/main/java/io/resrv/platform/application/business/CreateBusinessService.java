package io.resrv.platform.application.business;

import io.resrv.platform.application.business.in.CreateBusinessCommand;
import io.resrv.platform.application.business.in.CreateBusinessResult;
import io.resrv.platform.application.business.in.CreateBusinessUseCase;
import io.resrv.platform.application.business.out.BusinessCommandPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.application.membership.out.BusinessMembershipCommandPort;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessName;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.business.BusinessSlugAlreadyExistsException;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.shared.kernel.Timezone;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateBusinessService implements CreateBusinessUseCase {

    private final BusinessCommandPort businessCommandPort;
    private final BusinessQueryPort businessQueryPort;
    private final BusinessMembershipCommandPort membershipCommandPort;
    private final Clock clock;

    public CreateBusinessService(
            final BusinessCommandPort businessCommandPort,
            final BusinessQueryPort businessQueryPort,
            final BusinessMembershipCommandPort membershipCommandPort,
            final Clock clock) {
        this.businessCommandPort = businessCommandPort;
        this.businessQueryPort = businessQueryPort;
        this.membershipCommandPort = membershipCommandPort;
        this.clock = clock;
    }

    @Override
    public CreateBusinessResult create(final CreateBusinessCommand command) {
        final var slug = new BusinessSlug(command.slug());
        if (businessQueryPort.findBySlug(slug).isPresent()) {
            throw new BusinessSlugAlreadyExistsException(slug);
        }
        final var now = clock.instant();
        final var business =
                Business.create(
                        new BusinessName(command.name()),
                        slug,
                        Timezone.of(command.timezone()),
                        now);
        businessCommandPort.save(business);
        membershipCommandPort.save(
                BusinessMembership.owner(command.ownerAccountId(), business.id(), now));
        return CreateBusinessResult.from(business);
    }
}
