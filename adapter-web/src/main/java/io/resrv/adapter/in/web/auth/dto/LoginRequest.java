package io.resrv.adapter.in.web.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tenant administrator login credentials.")
public record LoginRequest(
        @Schema(
                        description = "Administrator email within the tenant.",
                        example = "owner@example.com",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String email,
        @Schema(
                        description = "Administrator password.",
                        example = "password123",
                        accessMode = Schema.AccessMode.WRITE_ONLY,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String password) {}
