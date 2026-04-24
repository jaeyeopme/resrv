package io.resrv.application.tenant;

import io.resrv.application.admin.out.AdminCommandPort;
import io.resrv.application.security.out.PasswordHashingPort;
import io.resrv.application.tenant.in.RegisterTenantCommand;
import io.resrv.application.tenant.in.RegisterTenantUseCase;
import io.resrv.application.tenant.out.TenantCommandPort;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.admin.Admin;
import io.resrv.domain.admin.Email;
import io.resrv.domain.tenant.CancellationWindow;
import io.resrv.domain.tenant.HoldTtl;
import io.resrv.domain.tenant.SlotDuration;
import io.resrv.domain.tenant.Slug;
import io.resrv.domain.tenant.SlugAlreadyExistsException;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantName;
import io.resrv.domain.tenant.Timezone;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RegisterTenantService implements RegisterTenantUseCase {

    private final Clock clock;
    private final TenantCommandPort tenantCommandPort;
    private final AdminCommandPort adminCommandPort;
    private final TenantQueryPort tenantQueryPort;
    private final PasswordHashingPort passwordHashingPort;

    RegisterTenantService(
            final Clock clock,
            final TenantCommandPort tenantCommandPort,
            final AdminCommandPort adminCommandPort,
            final TenantQueryPort tenantQueryPort,
            final PasswordHashingPort passwordHashingPort) {
        this.clock = clock;
        this.tenantCommandPort = tenantCommandPort;
        this.adminCommandPort = adminCommandPort;
        this.tenantQueryPort = tenantQueryPort;
        this.passwordHashingPort = passwordHashingPort;
    }

    @Override
    public Tenant register(final RegisterTenantCommand command) {
        final var slug = new Slug(command.slug());

        if (tenantQueryPort.existsBySlug(slug.value())) {
            throw new SlugAlreadyExistsException(slug);
        }

        final var holdTtl = HoldTtl.of(command.holdTtl());
        final var cancellationWindow = CancellationWindow.of(command.cancellationWindow());

        final var now = clock.instant();

        final var tenant =
                Tenant.create(
                        new TenantName(command.name()),
                        slug,
                        new Timezone(command.timezone()),
                        new SlotDuration(command.slotDuration()),
                        holdTtl,
                        cancellationWindow,
                        now);

        tenantCommandPort.save(tenant);

        final var hashedPassword = passwordHashingPort.hash(command.adminPassword());
        final var admin =
                Admin.create(tenant.id(), new Email(command.adminEmail()), hashedPassword, now);

        adminCommandPort.save(admin);

        return tenant;
    }
}
