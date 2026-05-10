package io.resrv.application.tenant.in;

import java.time.ZoneId;

public record RegisterTenantCommand(
        String name,
        String slug,
        ZoneId timezone,
        int slotDuration,
        Integer holdTtl,
        Integer cancellationWindow,
        String adminEmail,
        String adminPassword) {}
