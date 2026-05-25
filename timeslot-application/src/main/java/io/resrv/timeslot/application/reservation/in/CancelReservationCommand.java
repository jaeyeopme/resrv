package io.resrv.timeslot.application.reservation.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.timeslot.domain.reservation.ReservationCancellationActor;

public record CancelReservationCommand(
        BusinessId businessId,
        ReservationId reservationId,
        AccountId accountId,
        ReservationCancellationActor actor) {}
