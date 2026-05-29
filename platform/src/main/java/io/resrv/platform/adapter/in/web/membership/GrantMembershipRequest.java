package io.resrv.platform.adapter.in.web.membership;

import jakarta.validation.constraints.NotBlank;

record GrantMembershipRequest(
        @NotBlank(message = "Target account email is required") String targetAccountEmail) {}
