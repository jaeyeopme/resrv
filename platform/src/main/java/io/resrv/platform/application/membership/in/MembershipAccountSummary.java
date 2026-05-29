package io.resrv.platform.application.membership.in;

import io.resrv.platform.domain.account.Account;
import java.util.UUID;

public record MembershipAccountSummary(UUID accountId, String email, String name) {

    public static MembershipAccountSummary from(final Account account) {
        return new MembershipAccountSummary(
                account.id().value(), account.email().value(), account.name().value());
    }
}
