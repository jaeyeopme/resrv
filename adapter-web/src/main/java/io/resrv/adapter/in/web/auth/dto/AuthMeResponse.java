package io.resrv.adapter.in.web.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Identity claims extracted from the current Bearer token.")
public record AuthMeResponse(
        @Schema(
                        description = "Authenticated user identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                String userId,
        @Schema(
                        description = "Tenant boundary carried by the JWT.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91d")
                String tenantId,
        @Schema(description = "Application role carried by the JWT.", example = "OWNER")
                String role) {}
