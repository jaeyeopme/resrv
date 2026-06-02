package io.resrv.ticketing.application.inventory;

import io.resrv.ticketing.application.TicketingValidationException;
import io.resrv.ticketing.application.event.out.TicketEventQueryPort;
import io.resrv.ticketing.application.inventory.in.CreateTicketInventoryCommand;
import io.resrv.ticketing.application.inventory.in.GetTicketInventoryQuery;
import io.resrv.ticketing.application.inventory.in.TicketInventoryResult;
import io.resrv.ticketing.application.inventory.out.TicketInventoryCommandPort;
import io.resrv.ticketing.application.inventory.out.TicketInventoryQueryPort;
import io.resrv.ticketing.domain.inventory.TicketInventory;
import io.resrv.ticketing.domain.inventory.TicketInventoryTier;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketInventorySetupService {

    private final TicketEventQueryPort eventQueryPort;
    private final TicketInventoryCommandPort commandPort;
    private final TicketInventoryQueryPort queryPort;
    private final Clock clock;

    public TicketInventorySetupService(
            final TicketEventQueryPort eventQueryPort,
            final TicketInventoryCommandPort commandPort,
            final TicketInventoryQueryPort queryPort,
            final Clock clock) {
        this.eventQueryPort = eventQueryPort;
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.clock = clock;
    }

    @Transactional
    public TicketInventoryResult create(final CreateTicketInventoryCommand command) {
        final var event =
                eventQueryPort
                        .findById(command.ticketEventId())
                        .orElseThrow(
                                () ->
                                        new TicketingValidationException(
                                                "Ticket event is not available: "
                                                        + command.ticketEventId().value()));
        final var tiers =
                command.tiers().stream()
                        .map(
                                tier ->
                                        TicketInventoryTier.create(
                                                tier.displayName(),
                                                tier.total(),
                                                tier.reserved(),
                                                tier.confirmed(),
                                                tier.softReserved()))
                        .toList();
        final var inventory = TicketInventory.create(event, tiers, clock.instant());
        commandPort.save(inventory);
        return TicketInventoryResult.from(inventory);
    }

    @Transactional(readOnly = true)
    public Optional<TicketInventoryResult> find(final GetTicketInventoryQuery query) {
        return queryPort.findById(query.ticketInventoryId()).map(TicketInventoryResult::from);
    }
}
