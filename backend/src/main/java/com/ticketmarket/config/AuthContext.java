package com.ticketmarket.config;

public final class AuthContext {
    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthUser authUser) {
        CURRENT.set(authUser);
    }

    public static Long currentUserId() {
        AuthUser authUser = CURRENT.get();
        return authUser == null ? null : authUser.userId();
    }

    public static String currentRole() {
        AuthUser authUser = CURRENT.get();
        return authUser == null ? null : authUser.roleCode();
    }

    public static AuthUser currentUser() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record AuthUser(Long userId, String username, String roleCode) {
    }
}
