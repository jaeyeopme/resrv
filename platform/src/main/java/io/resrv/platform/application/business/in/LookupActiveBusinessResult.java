package io.resrv.platform.application.business.in;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.Objects;

public record LookupActiveBusinessResult(
        BusinessId id, String name, String slug, Timezone timezone) {

    public LookupActiveBusinessResult {
        Objects.requireNonNull(id, "Business id must not be null");
        Objects.requireNonNull(name, "Business name must not be null");
        Objects.requireNonNull(slug, "Business slug must not be null");
        Objects.requireNonNull(timezone, "Business timezone must not be null");
    }
}
