package io.resrv.application.tenant.in;

import java.time.ZoneId;
import org.jspecify.annotations.Nullable;

public record RegisterTenantCommand(
        String name,
        String slug,
        ZoneId timezone,
        int slotDuration,
        @Nullable Integer holdTtl,
        @Nullable Integer cancellationWindow,
        String adminEmail,
        String adminPassword) {}
