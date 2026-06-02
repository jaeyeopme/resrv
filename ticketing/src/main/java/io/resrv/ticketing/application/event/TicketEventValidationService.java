package io.resrv.ticketing.application.event;

import io.resrv.ticketing.domain.event.TicketEventProfile;
import io.resrv.ticketing.domain.event.TicketSaleWindow;
import org.springframework.stereotype.Service;

@Service
public class TicketEventValidationService {

    public void validate(final TicketEventProfile profile, final TicketSaleWindow saleWindow) {
        if (!saleWindow.endAt().isAfter(saleWindow.startAt())) {
            throw new IllegalArgumentException(
                    "Sale window start time must be before sale window end time");
        }
        if (!profile.eventEndAt().isAfter(profile.eventStartAt())) {
            throw new IllegalArgumentException("Event start time must be before event end time");
        }
    }
}
