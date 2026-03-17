package com.alikeyou.itmodulecommon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "修改密码请求DTO")
public class ChangePasswordDTO {
    @Schema(description = "旧密码")
    private String oldPassword;
    
    @Schema(description = "新密码")
    private String newPassword;
    
    @Schema(description = "确认新密码")
    private String confirmPassword;

    // getter and setter
    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}