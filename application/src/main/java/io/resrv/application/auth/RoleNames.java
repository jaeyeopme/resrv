package io.resrv.application.auth;

import java.util.Set;

public final class RoleNames {

    public static final String OWNER = "OWNER";
    public static final String STAFF = "STAFF";
    public static final String CUSTOMER = "CUSTOMER";

    private static final Set<String> ADMIN_ROLES = Set.of(OWNER, STAFF);

    private RoleNames() {}

    public static boolean isAdmin(final String role) {
        return ADMIN_ROLES.contains(role);
    }
}
