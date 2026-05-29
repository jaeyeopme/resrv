package io.resrv.platform.application.membership.in;

import io.resrv.platform.domain.membership.BusinessRole;

public record MembershipState(BusinessRole role, boolean active) {}
