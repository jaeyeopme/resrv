package io.resrv.ticketing.application.activity.in;

import io.resrv.shared.kernel.AccountId;
import java.util.Objects;

public record CustomerTicketHistoryQuery(AccountId customerAccountId) {

    public CustomerTicketHistoryQuery {
        Objects.requireNonNull(customerAccountId, "Customer account id must not be null");
    }
}
