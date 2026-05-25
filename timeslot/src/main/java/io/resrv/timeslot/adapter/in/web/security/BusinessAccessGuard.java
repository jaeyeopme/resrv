package io.resrv.timeslot.adapter.in.web.security;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.reservation.ReservationAccessDeniedException;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class BusinessAccessGuard {

    private BusinessAccessGuard() {}

    public static void requireAccess(
            final BusinessAccessPort businessAccessPort,
            final JwtAuthenticationToken authentication,
            final UUID businessId) {
        final var account = AuthenticatedAccount.from(authentication);
        final var parsedBusinessId = BusinessId.of(businessId);
        if (!businessAccessPort.hasBusinessAccess(account.accountId(), parsedBusinessId)) {
            throw new ReservationAccessDeniedException();
        }
    }
}
