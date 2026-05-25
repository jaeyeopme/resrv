package io.resrv.timeslot.application.reservation.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;

public record HoldReservationCommand(
        BusinessId businessId, ResourceId resourceId, AccountId accountId, String slotId) {}
