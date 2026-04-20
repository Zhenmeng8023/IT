package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.entity.Permission;
import com.alikeyou.itmodulecommon.entity.Role;
import com.alikeyou.itmodulecommon.repository.MenuRepository;
import com.alikeyou.itmodulecommon.repository.RoleRepository;
import com.alikeyou.itmodulecommon.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Page<Role> getRolesPage(String keyword, Pageable pageable) {
        return roleRepository.searchAdminRoles(keyword, pageable);
    }

    @Override
    public Optional<Role> getRoleById(Integer id) {
        return roleRepository.findById(id);
    }

    @Override
    public Role createRole(Role role) {
        Instant now = Instant.now();
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        return roleRepository.save(role);
    }

    @Override
    public Role updateRole(Integer id, Role role) {
        Optional<Role> existingRole = roleRepository.findById(id);
        if (existingRole.isPresent()) {
            Role updatedRole = existingRole.get();
            updatedRole.setRoleName(role.getRoleName());
            updatedRole.setDescription(role.getDescription());
            updatedRole.setUpdatedAt(Instant.now());
            return roleRepository.save(updatedRole);
        }
        throw new RuntimeException("Role not found with id: " + id);
    }

    @Override
    public void deleteRole(Integer id) {
        if (!roleRepository.existsById(id)) {
            throw new RuntimeException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }

    @Override
    public void assignMenus(Integer roleId, List<Integer> menuIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        List<Menu> menus = menuIds == null || menuIds.isEmpty()
                ? List.of()
                : menuRepository.findAllById(menuIds);

        role.getMenus().clear();
        role.getMenus().addAll(menus);
        role.setUpdatedAt(Instant.now());
        roleRepository.save(role);
    }

    @Override
    public void assignPermissions(Integer roleId, List<Integer> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        List<Menu> menus = permissionIds == null || permissionIds.isEmpty()
                ? List.of()
                : menuRepository.findByPermissionIdIn(permissionIds);

        role.getMenus().clear();
        role.getMenus().addAll(menus);
        role.setUpdatedAt(Instant.now());
        roleRepository.save(role);
    }

    @Override
    public List<Menu> getRoleMenus(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));
        return new ArrayList<>(role.getMenus());
    }

    @Override
    public List<Permission> getRolePermissions(Integer roleId) {
        List<Menu> menus = getRoleMenus(roleId);
        LinkedHashMap<Integer, Permission> dedup = new LinkedHashMap<>();
        for (Menu menu : menus) {
            Permission permission = menu.getPermission();
            if (permission != null && permission.getId() != null) {
                dedup.put(permission.getId(), permission);
            }
        }
        List<Permission> permissions = new ArrayList<>(dedup.values());
        permissions.sort(Comparator.comparing(Permission::getId));
        return permissions;
    }
}
