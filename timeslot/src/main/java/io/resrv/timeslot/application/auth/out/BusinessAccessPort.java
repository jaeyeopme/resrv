package io.resrv.timeslot.application.auth.out;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;

/**
 * Timeslot authorization port for business-scoped owner/staff actions.
 *
 * <p>This port answers whether the caller may act on the business. It must not be used as a
 * business summary lookup or as customer reservation ownership authorization.
 */
public interface BusinessAccessPort {

    boolean hasBusinessAccess(AccountId accountId, BusinessId businessId);
}
