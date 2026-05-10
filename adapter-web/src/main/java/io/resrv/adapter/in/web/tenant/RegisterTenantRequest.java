package io.resrv.adapter.in.web.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import org.jspecify.annotations.Nullable;

@Schema(description = "Tenant onboarding payload including the first OWNER administrator.")
record RegisterTenantRequest(
        @Schema(description = "Tenant display name.", example = "Demo Studio")
                @NotBlank(message = "Name is required")
                @Size(max = 100, message = "Name must be 1-100 characters")
                String name,
        @Schema(
                        description = "Public tenant slug used by public login/signup URLs.",
                        example = "demo-studio")
                @NotBlank(message = "Slug is required")
                String slug,
        @Schema(description = "IANA timezone used for slot calculation.", example = "Asia/Seoul")
                @NotNull(message = "Timezone is required")
                ZoneId timezone,
        @Schema(description = "Default slot duration in minutes.", example = "60")
                @NotNull(message = "Slot duration is required")
                @Min(value = 30, message = "Slot duration must be at least 30 minutes")
                @Max(value = 480, message = "Slot duration must be at most 480 minutes")
                Integer slotDuration,
        @Schema(description = "Reservation hold TTL in minutes.", example = "15")
                @Nullable
                @Min(value = 5, message = "Hold TTL must be at least 5 minutes")
                @Max(value = 30, message = "Hold TTL must be at most 30 minutes")
                Integer holdTtl,
        @Schema(
                        description =
                                "Minimum minutes before start time when cancellation is allowed.",
                        example = "60")
                @Nullable
                @Min(value = 0, message = "Cancellation window must be 0 or more minutes")
                Integer cancellationWindow,
        @Schema(description = "First tenant administrator.")
                @NotNull(message = "Admin information is required")
                @Valid
                AdminRequest admin) {

    @Schema(description = "First OWNER administrator credentials.")
    record AdminRequest(
            @Schema(description = "Administrator email.", example = "owner@example.com")
                    @NotBlank(message = "Admin email is required")
                    @Email(message = "Must be a valid email address")
                    String email,
            @Schema(
                            description = "Administrator password.",
                            example = "password123",
                            accessMode = Schema.AccessMode.WRITE_ONLY)
                    @NotBlank(message = "Admin password is required")
                    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
                    String password) {}
}
