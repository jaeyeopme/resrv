package io.resrv.platform.adapter.in.web.membership;

import io.resrv.platform.domain.membership.BusinessRole;
import jakarta.validation.constraints.NotNull;

record UpdateMembershipRoleRequest(
        @NotNull(message = "Membership role is required") BusinessRole role) {}
