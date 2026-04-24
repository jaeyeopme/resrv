package io.resrv.adapter.in.web.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import org.jspecify.annotations.Nullable;

record RegisterTenantRequest(
        @NotBlank(message = "Name is required")
                @Size(max = 100, message = "Name must be 1-100 characters")
                String name,
        @NotBlank(message = "Slug is required") String slug,
        @NotNull(message = "Timezone is required") ZoneId timezone,
        @NotNull(message = "Slot duration is required")
                @Min(value = 30, message = "Slot duration must be at least 30 minutes")
                @Max(value = 480, message = "Slot duration must be at most 480 minutes")
                Integer slotDuration,
        @Nullable
                @Min(value = 5, message = "Hold TTL must be at least 5 minutes")
                @Max(value = 30, message = "Hold TTL must be at most 30 minutes")
                Integer holdTtl,
        @Nullable @Min(value = 0, message = "Cancellation window must be 0 or more minutes")
                Integer cancellationWindow,
        @NotNull(message = "Admin information is required") @Valid AdminRequest admin) {

    record AdminRequest(
            @NotBlank(message = "Admin email is required")
                    @Email(message = "Must be a valid email address")
                    String email,
            @NotBlank(message = "Admin password is required")
                    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
                    String password) {}
}
