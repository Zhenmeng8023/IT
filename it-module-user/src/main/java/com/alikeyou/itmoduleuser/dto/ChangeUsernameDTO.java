package com.alikeyou.itmoduleuser.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "修改用户名请求DTO")
public class ChangeUsernameDTO {
    @Schema(description = "新用户名")
    private String newUsername;

    // getter and setter
    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }
}