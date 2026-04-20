package com.alikeyou.itmodulecommon.dto.admin.rbac;

import com.alikeyou.itmodulecommon.entity.Menu;
import com.fasterxml.jackson.annotation.JsonAlias;

public class AdminRbacMenuUpsertRequest {

    private String name;

    private String path;

    private String component;

    private String icon;

    @JsonAlias("sort_order")
    private Integer sortOrder;

    @JsonAlias({"is_hidden", "hidden"})
    private Boolean isHidden;

    @JsonAlias("parent_id")
    private Integer parentId;

    @JsonAlias("permission_id")
    private Integer permissionId;

    private String type;

    public Menu toMenu() {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setIcon(icon);
        menu.setSortOrder(sortOrder);
        menu.setIsHidden(isHidden);
        menu.setParentId(parentId);
        menu.setPermissionId(permissionId);
        menu.setType(type);

        if ("button".equals(type)) {
            menu.setPath(null);
            menu.setComponent(null);
        }

        return menu;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsHidden() {
        return isHidden;
    }

    public void setIsHidden(Boolean hidden) {
        isHidden = hidden;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
