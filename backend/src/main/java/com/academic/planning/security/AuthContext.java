package com.academic.planning.security;

public final class AuthContext {

    private static final ThreadLocal<SessionUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(SessionUser user) {
        CURRENT.set(user);
    }

    public static SessionUser requireUser() {
        SessionUser user = CURRENT.get();
        if (user == null) {
            throw new AuthException(401, "请先登录");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
