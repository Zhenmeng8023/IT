package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.entity.Permission;
import com.alikeyou.itmodulecommon.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RoleService {

    List<Role> getAllRoles();

    Page<Role> getRolesPage(String keyword, Pageable pageable);

    Optional<Role> getRoleById(Integer id);

    Role createRole(Role role);

    Role updateRole(Integer id, Role role);

    void deleteRole(Integer id);

    void assignMenus(Integer roleId, List<Integer> menuIds);

    void assignPermissions(Integer roleId, List<Integer> permissionIds);

    List<Menu> getRoleMenus(Integer roleId);

    List<Permission> getRolePermissions(Integer roleId);
}
