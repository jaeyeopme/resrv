package io.resrv.platform.application.business.in;

import io.resrv.shared.kernel.AccountId;
import java.util.Objects;

public record CreateBusinessCommand(
        AccountId ownerAccountId, String name, String slug, String timezone) {

    public CreateBusinessCommand {
        Objects.requireNonNull(ownerAccountId, "Owner account id must not be null");
    }
}
