package io.resrv.platform.exchange.business;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.Objects;

public record ActiveBusinessView(BusinessId id, String name, String slug, Timezone timezone) {

    public ActiveBusinessView {
        Objects.requireNonNull(id, "Business id must not be null");
        Objects.requireNonNull(name, "Business name must not be null");
        Objects.requireNonNull(slug, "Business slug must not be null");
        Objects.requireNonNull(timezone, "Business timezone must not be null");
    }
}
