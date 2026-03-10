package com.alikeyou.itmodulelogin.dto;

/**
 * 登录请求DTO
 * 用于登录和注册
 */
public class LoginRequest {
    private String username; // 用户名
    private String password; // 密码
    private String email; // 邮箱

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}