package io.resrv.adapter.in.web.customer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Customer login credentials.")
record CustomerLoginRequest(
        @Schema(
                        description = "Customer email within the tenant.",
                        example = "customer@example.com",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String email,
        @Schema(
                        description = "Customer password.",
                        example = "password123",
                        accessMode = Schema.AccessMode.WRITE_ONLY,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String password) {}
