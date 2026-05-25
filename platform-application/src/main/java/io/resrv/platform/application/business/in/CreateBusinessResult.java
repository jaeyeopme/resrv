package io.resrv.platform.application.business.in;

import io.resrv.platform.domain.business.Business;
import java.util.UUID;

public record CreateBusinessResult(UUID id, String name, String slug, String timezone) {

    public static CreateBusinessResult from(final Business business) {
        return new CreateBusinessResult(
                business.id().value(),
                business.name().value(),
                business.slug().value(),
                business.timezone().value().getId());
    }
}
