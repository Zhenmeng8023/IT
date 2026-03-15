package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Permission;
import com.alikeyou.itmodulecommon.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    List<Role> getAllRoles();
    Optional<Role> getRoleById(Integer id);
    Role createRole(Role role);
    Role updateRole(Integer id, Role role);
    void deleteRole(Integer id);
    void assignPermissions(Integer roleId, List<Integer> permissionIds);
    List<Permission> getRolePermissions(Integer roleId);
}