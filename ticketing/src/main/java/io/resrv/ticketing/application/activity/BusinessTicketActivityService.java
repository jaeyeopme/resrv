package io.resrv.ticketing.application.activity;

import io.resrv.ticketing.application.activity.in.BusinessTicketActivityQuery;
import io.resrv.ticketing.application.activity.in.BusinessTicketActivityResult;
import io.resrv.ticketing.application.activity.out.TicketPurchaseActivityQueryPort;
import io.resrv.ticketing.application.event.out.TicketEventQueryPort;
import io.resrv.ticketing.application.platform.out.TicketingBusinessAccessPort;
import io.resrv.ticketing.application.purchase.TicketPurchaseAccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessTicketActivityService {

    private final TicketEventQueryPort eventQueryPort;
    private final TicketingBusinessAccessPort businessAccessPort;
    private final TicketPurchaseActivityQueryPort activityQueryPort;

    public BusinessTicketActivityService(
            final TicketEventQueryPort eventQueryPort,
            final TicketingBusinessAccessPort businessAccessPort,
            final TicketPurchaseActivityQueryPort activityQueryPort) {
        this.eventQueryPort = eventQueryPort;
        this.businessAccessPort = businessAccessPort;
        this.activityQueryPort = activityQueryPort;
    }

    @Transactional(readOnly = true)
    public BusinessTicketActivityResult list(final BusinessTicketActivityQuery query) {
        final var event =
                eventQueryPort
                        .findById(query.ticketEventId())
                        .orElseThrow(
                                () ->
                                        new TicketPurchaseAccessDeniedException(
                                                "Ticket event not found"));
        if (!businessAccessPort.hasBusinessAccess(query.actorAccountId(), event.businessId())) {
            throw new TicketPurchaseAccessDeniedException("Ticket event not found");
        }
        return BusinessTicketActivityResult.from(
                event.id().value(),
                activityQueryPort.findBusinessEventPurchases(event.businessId(), event.id()));
    }
}
