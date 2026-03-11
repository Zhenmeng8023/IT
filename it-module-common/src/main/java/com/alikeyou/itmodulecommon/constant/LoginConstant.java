package com.alikeyou.itmodulecommon.constant;

public class LoginConstant {
    //登录常量
    
    //用户识别信息
    public static final String USER_NAME = "username";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String USER_ID = "userId";
    public static final String ROLE_ID = "roleId";
    
    //存储用户信息
    private static String username;
    private static String password;
    private static String email;
    private static Long userId; // 用户唯一ID
    private static Long roleId; // 角色身份ID
    
    //setter方法
    public static void setUsername(String username) {
        LoginConstant.username = username;
    }
    
    public static void setPassword(String password) {
        LoginConstant.password = password;
    }
    
    public static void setEmail(String email) {
        LoginConstant.email = email;
    }
    
    public static void setUserId(Long userId) {
        LoginConstant.userId = userId;
    }
    
    public static void setRoleId(Long roleId) {
        LoginConstant.roleId = roleId;
    }
    
    //getter方法
    public static String getUsername() {
        return username;
    }
    
    public static String getPassword() {
        return password;
    }
    
    public static String getEmail() {
        return email;
    }
    
    public static Long getUserId() {
        return userId;
    }
    
    public static Long getRoleId() {
        return roleId;
    }
    
    //清空用户信息
    public static void clearUserInfo() {
        username = null;
        password = null;
        email = null;
        userId = null;
        roleId = null;
    }
}