package io.resrv.timeslot.application.discovery.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.ResourceId;
import java.util.Objects;

public record HoldReservationByBusinessSlugCommand(
        String businessSlug, ResourceId resourceId, AccountId accountId, String slotId) {

    public HoldReservationByBusinessSlugCommand {
        Objects.requireNonNull(businessSlug, "Business slug must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(accountId, "Account id must not be null");
        Objects.requireNonNull(slotId, "Slot id must not be null");
    }
}
