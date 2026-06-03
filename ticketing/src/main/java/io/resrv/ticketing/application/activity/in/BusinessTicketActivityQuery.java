package io.resrv.ticketing.application.activity.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.event.TicketEventId;
import java.util.Objects;

public record BusinessTicketActivityQuery(AccountId actorAccountId, TicketEventId ticketEventId) {

    public BusinessTicketActivityQuery {
        Objects.requireNonNull(actorAccountId, "Actor account id must not be null");
        Objects.requireNonNull(ticketEventId, "Ticket event id must not be null");
    }
}
