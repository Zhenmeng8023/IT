package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    List<Role> getAllRoles();
    Optional<Role> getRoleById(Integer id);
    Role createRole(Role role);
    Role updateRole(Integer id, Role role);
    void deleteRole(Integer id);
    // 为角色分配菜单权限（包括按钮）
    void assignMenus(Integer roleId, List<Integer> menuIds);
    List<Menu> getRoleMenus(Integer roleId);
}