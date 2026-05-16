package io.resrv.adapter.in.web.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer registration payload.")
record RegisterCustomerRequest(
        @Schema(description = "Customer email within the tenant.", example = "customer@example.com")
                @NotBlank(message = "Customer email is required")
                @Email(message = "Must be a valid email address")
                String email,
        @Schema(description = "Customer display name.", example = "Jane Customer")
                @NotBlank(message = "Customer name is required")
                @Size(max = 100, message = "Customer name must be at most 100 characters")
                String name,
        @Schema(
                        description = "Customer password.",
                        example = "password123",
                        accessMode = Schema.AccessMode.WRITE_ONLY)
                @NotBlank(message = "Customer password is required")
                @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
                String password) {}
