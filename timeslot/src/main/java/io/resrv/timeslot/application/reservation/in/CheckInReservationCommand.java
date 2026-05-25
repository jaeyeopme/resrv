package io.resrv.timeslot.application.reservation.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;

public record CheckInReservationCommand(
        BusinessId businessId, ReservationId reservationId, AccountId accountId) {}
