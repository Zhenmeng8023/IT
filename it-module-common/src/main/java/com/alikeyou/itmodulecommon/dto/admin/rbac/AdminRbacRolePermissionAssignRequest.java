package com.alikeyou.itmodulecommon.dto.admin.rbac;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public class AdminRbacRolePermissionAssignRequest {

    @JsonAlias({"permission_ids", "permissions"})
    private List<Integer> permissionIds;

    public List<Integer> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Integer> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
