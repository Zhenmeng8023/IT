package com.alikeyou.itmodulelogin.dto;

/**
 * 验证码请求DTO
 * 用于请求发送邮箱验证码
 */
public class VerifyCodeRequest {
    private String email; // 邮箱地址

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}