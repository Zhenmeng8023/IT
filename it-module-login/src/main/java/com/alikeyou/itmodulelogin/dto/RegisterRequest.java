package com.alikeyou.itmodulelogin.dto;

/**
 * 注册请求DTO
 * 用于接收注册请求的参数
 */
public class RegisterRequest {
    private String name; // 用户名
    private String password; // 密码
    private String email; // 邮箱
    private String code; // 验证码

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}