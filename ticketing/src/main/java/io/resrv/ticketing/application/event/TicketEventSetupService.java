package io.resrv.ticketing.application.event;

import io.resrv.ticketing.application.TicketingValidationException;
import io.resrv.ticketing.application.event.in.CreateTicketEventCommand;
import io.resrv.ticketing.application.event.in.GetTicketEventQuery;
import io.resrv.ticketing.application.event.in.TicketEventResult;
import io.resrv.ticketing.application.event.out.TicketEventCommandPort;
import io.resrv.ticketing.application.event.out.TicketEventQueryPort;
import io.resrv.ticketing.application.platform.out.TicketingBusinessAccessPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventProfile;
import io.resrv.ticketing.domain.event.TicketSaleWindow;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketEventSetupService {

    private final TicketingBusinessAccessPort businessAccessPort;
    private final TicketEventCommandPort commandPort;
    private final TicketEventQueryPort queryPort;
    private final TicketEventValidationService validationService;
    private final Clock clock;

    public TicketEventSetupService(
            final TicketingBusinessAccessPort businessAccessPort,
            final TicketEventCommandPort commandPort,
            final TicketEventQueryPort queryPort,
            final TicketEventValidationService validationService,
            final Clock clock) {
        this.businessAccessPort = businessAccessPort;
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.validationService = validationService;
        this.clock = clock;
    }

    @Transactional
    public TicketEventResult create(final CreateTicketEventCommand command) {
        businessAccessPort
                .findActiveBusiness(command.businessId())
                .orElseThrow(
                        () ->
                                new TicketingValidationException(
                                        "Business is not available: "
                                                + command.businessId().value()));
        final var profile =
                new TicketEventProfile(
                        command.title(),
                        command.eventStartAt(),
                        command.eventEndAt(),
                        command.eventTimezone());
        final var saleWindow =
                new TicketSaleWindow(
                        command.saleStartAt(), command.saleEndAt(), command.saleTimezone());
        validationService.validate(profile, saleWindow);
        final var event =
                TicketEvent.create(command.businessId(), profile, saleWindow, clock.instant());
        commandPort.save(event);
        return TicketEventResult.from(event);
    }

    @Transactional(readOnly = true)
    public Optional<TicketEventResult> find(final GetTicketEventQuery query) {
        return queryPort.findById(query.ticketEventId()).map(TicketEventResult::from);
    }
}
