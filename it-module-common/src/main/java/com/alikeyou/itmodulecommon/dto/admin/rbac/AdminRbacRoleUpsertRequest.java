package com.alikeyou.itmodulecommon.dto.admin.rbac;

import com.fasterxml.jackson.annotation.JsonAlias;

public class AdminRbacRoleUpsertRequest {

    @JsonAlias({"role_name", "name"})
    private String roleName;

    private String description;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
