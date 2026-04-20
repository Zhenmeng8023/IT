package com.alikeyou.itmodulecommon.dto.admin.rbac;

import com.alikeyou.itmodulecommon.entity.Role;

import java.time.Instant;

public class AdminRbacRoleDTO {

    private Integer id;
    private String roleName;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public static AdminRbacRoleDTO from(Role role) {
        AdminRbacRoleDTO dto = new AdminRbacRoleDTO();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setDescription(role.getDescription());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
