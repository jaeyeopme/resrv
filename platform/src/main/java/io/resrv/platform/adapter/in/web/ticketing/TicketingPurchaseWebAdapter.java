package io.resrv.platform.adapter.in.web.ticketing;

import io.resrv.platform.adapter.in.web.security.AuthenticatedAccount;
import io.resrv.ticketing.application.activity.BusinessTicketActivityService;
import io.resrv.ticketing.application.activity.CustomerTicketHistoryService;
import io.resrv.ticketing.application.activity.in.BusinessTicketActivityQuery;
import io.resrv.ticketing.application.activity.in.CustomerTicketHistoryQuery;
import io.resrv.ticketing.application.purchase.TicketPurchaseConfirmationService;
import io.resrv.ticketing.application.purchase.in.ConfirmTicketPurchaseCommand;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyKey;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ticketing")
class TicketingPurchaseWebAdapter implements TicketingPurchaseApiDocs {

    private final TicketPurchaseConfirmationService purchaseConfirmationService;
    private final CustomerTicketHistoryService customerTicketHistoryService;
    private final BusinessTicketActivityService businessTicketActivityService;

    TicketingPurchaseWebAdapter(
            final TicketPurchaseConfirmationService purchaseConfirmationService,
            final CustomerTicketHistoryService customerTicketHistoryService,
            final BusinessTicketActivityService businessTicketActivityService) {
        this.purchaseConfirmationService = purchaseConfirmationService;
        this.customerTicketHistoryService = customerTicketHistoryService;
        this.businessTicketActivityService = businessTicketActivityService;
    }

    @Override
    @PostMapping("/events/{ticketEventId}/purchases")
    public ResponseEntity<TicketPurchaseResponse> confirm(
            final JwtAuthenticationToken authentication,
            @PathVariable final UUID ticketEventId,
            @Valid @RequestBody final ConfirmTicketPurchaseRequest request) {
        final var account = AuthenticatedAccount.from(authentication);
        final var result =
                purchaseConfirmationService.confirm(
                        new ConfirmTicketPurchaseCommand(
                                TicketEventId.of(ticketEventId),
                                account.accountId(),
                                request.seatIds().stream().map(TicketSeatId::of).toList(),
                                PurchaseConfirmationIdempotencyKey.of(request.idempotencyKey())));
        if (!result.purchased()) {
            return ResponseEntity.badRequest().body(TicketPurchaseResponse.from(result));
        }
        return ResponseEntity.created(URI.create("/api/ticketing/purchases/" + result.id()))
                .body(TicketPurchaseResponse.from(result));
    }

    @Override
    @GetMapping("/customers/me/purchases")
    public CustomerTicketHistoryResponse customerHistory(
            final JwtAuthenticationToken authentication) {
        final var account = AuthenticatedAccount.from(authentication);
        return CustomerTicketHistoryResponse.from(
                customerTicketHistoryService.list(
                        new CustomerTicketHistoryQuery(account.accountId())));
    }

    @Override
    @GetMapping("/business/events/{ticketEventId}/purchases")
    public BusinessTicketActivityResponse businessActivity(
            final JwtAuthenticationToken authentication, @PathVariable final UUID ticketEventId) {
        final var account = AuthenticatedAccount.from(authentication);
        return BusinessTicketActivityResponse.from(
                businessTicketActivityService.list(
                        new BusinessTicketActivityQuery(
                                account.accountId(), TicketEventId.of(ticketEventId))));
    }
}
