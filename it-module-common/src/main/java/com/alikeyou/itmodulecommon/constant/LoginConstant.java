package com.alikeyou.itmodulecommon.constant;

public final class LoginConstant {

    public static final String USER_NAME = "username";
    public static final String EMAIL = "email";
    public static final String USER_ID = "userId";
    public static final String ROLE_ID = "roleId";

    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE_ID_HOLDER = new ThreadLocal<>();

    private LoginConstant() {
    }

    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    public static void setEmail(String email) {
        EMAIL_HOLDER.set(email);
    }

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static void setRoleId(Integer roleId) {
        ROLE_ID_HOLDER.set(roleId);
    }

    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    public static String getEmail() {
        return EMAIL_HOLDER.get();
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static Integer getRoleId() {
        return ROLE_ID_HOLDER.get();
    }

    public static void clearUserInfo() {
        USERNAME_HOLDER.remove();
        EMAIL_HOLDER.remove();
        USER_ID_HOLDER.remove();
        ROLE_ID_HOLDER.remove();
    }
}
