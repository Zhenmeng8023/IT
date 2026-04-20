package com.alikeyou.itmodulecommon.dto.admin.rbac;

import com.alikeyou.itmodulecommon.entity.Permission;

import java.time.Instant;

public class AdminRbacPermissionDTO {

    private Integer id;
    private String permissionCode;
    private String description;
    private Instant createdAt;

    public static AdminRbacPermissionDTO from(Permission permission) {
        AdminRbacPermissionDTO dto = new AdminRbacPermissionDTO();
        dto.setId(permission.getId());
        dto.setPermissionCode(permission.getPermissionCode());
        dto.setDescription(permission.getDescription());
        dto.setCreatedAt(permission.getCreatedAt());
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
