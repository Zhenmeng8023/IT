package com.alikeyou.itmodulecommon.dto.admin.rbac;

import com.fasterxml.jackson.annotation.JsonAlias;

public class AdminRbacPermissionUpsertRequest {

    @JsonAlias({"permission_code", "code"})
    private String permissionCode;

    private String description;

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
