package com.alikeyou.itmodulecommon.dto.admin.rbac;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public class AdminRbacRoleMenuAssignRequest {

    @JsonAlias({"menu_ids", "menus"})
    private List<Integer> menuIds;

    public List<Integer> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Integer> menuIds) {
        this.menuIds = menuIds;
    }
}
