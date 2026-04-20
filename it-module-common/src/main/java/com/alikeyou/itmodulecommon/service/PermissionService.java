package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PermissionService {

    List<Permission> getAllPermissions();

    Page<Permission> getPermissionsPage(String keyword, Pageable pageable);

    Optional<Permission> getPermissionById(Integer id);

    Permission createPermission(Permission permission);

    Permission updatePermission(Integer id, Permission permission);

    void deletePermission(Integer id);
}
