package com.alikeyou.itmoduleuser.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "修改邮箱请求DTO")
public class ChangeEmailDTO {
    @Schema(description = "新邮箱地址")
    private String newEmail;
    
    @Schema(description = "验证码")
    private String verificationCode;

    // getter and setter
    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}