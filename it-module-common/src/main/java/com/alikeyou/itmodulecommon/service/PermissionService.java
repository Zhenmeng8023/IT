package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionService {
    List<Permission> getAllPermissions();
    Optional<Permission> getPermissionById(Integer id);
    Permission createPermission(Permission permission);
    Permission updatePermission(Integer id, Permission permission);
    void deletePermission(Integer id);
}
