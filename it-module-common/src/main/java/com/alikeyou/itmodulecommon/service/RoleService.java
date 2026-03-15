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
    
    // 为角色分配菜单权限
    void assignMenus(Integer roleId, List<Integer> menuIds);
    
    // 为角色分配按钮权限
    void assignButtons(Integer roleId, List<Integer> buttonIds);
}