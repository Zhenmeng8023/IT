package com.alikeyou.itmoduleuser.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "验证密码请求DTO")
public class VerifyPasswordDTO {
    @Schema(description = "密码")
    private String password;

    // getter and setter
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}