package io.resrv.domain.tenant;

import java.time.ZoneId;
import java.util.Objects;

public record Timezone(ZoneId value) {

    public Timezone {
        Objects.requireNonNull(value, "Timezone must not be null");
    }
}
