package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.entity.Permission;
import com.alikeyou.itmodulecommon.entity.Role;
import com.alikeyou.itmodulecommon.repository.MenuRepository;
import com.alikeyou.itmodulecommon.repository.PermissionRepository;
import com.alikeyou.itmodulecommon.repository.RoleRepository;
import com.alikeyou.itmodulecommon.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private MenuRepository menuRepository;

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Optional<Role> getRoleById(Integer id) {
        return roleRepository.findById(id);
    }

    @Override
    public Role createRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public Role updateRole(Integer id, Role role) {
        Optional<Role> existingRole = roleRepository.findById(id);
        if (existingRole.isPresent()) {
            Role updatedRole = existingRole.get();
            updatedRole.setRoleName(role.getRoleName());
            updatedRole.setDescription(role.getDescription());
            return roleRepository.save(updatedRole);
        } else {
            throw new RuntimeException("Role not found with id: " + id);
        }
    }

    @Override
    public void deleteRole(Integer id) {
        if (roleRepository.existsById(id)) {
            roleRepository.deleteById(id);
        } else {
            throw new RuntimeException("Role not found with id: " + id);
        }
    }

    @Override
    public void assignPermissions(Integer roleId, List<Integer> permissionIds) {
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            List<Permission> permissions = permissionRepository.findAllById(permissionIds);
            role.getPermissions().clear();
            role.getPermissions().addAll(permissions);
            roleRepository.save(role);
        } else {
            throw new RuntimeException("Role not found with id: " + roleId);
        }
    }

    @Override
    public List<Permission> getRolePermissions(Integer roleId) {
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            return role.getPermissions().stream().collect(java.util.stream.Collectors.toList());
        } else {
            throw new RuntimeException("Role not found with id: " + roleId);
        }
    }

    @Override
    public void assignMenus(Integer roleId, List<Integer> menuIds) {
        // 根据菜单ID查询对应的菜单
        List<Menu> menus = menuRepository.findAllById(menuIds);
        
        // 提取菜单关联的权限ID
        List<Integer> permissionIds = new java.util.ArrayList<>();
        for (Menu menu : menus) {
            if (menu.getPermission() != null && menu.getPermission().getId() != null) {
                permissionIds.add(menu.getPermission().getId());
            }
        }
        
        // 为角色分配这些权限
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            List<Permission> permissions = permissionRepository.findAllById(permissionIds);
            // 将这些权限添加到角色的权限集合中
            role.getPermissions().addAll(permissions);
            roleRepository.save(role);
        } else {
            throw new RuntimeException("Role not found with id: " + roleId);
        }
    }

    @Override
    public void assignButtons(Integer roleId, List<Integer> buttonIds) {
        // 1. 检查roleId是否存在
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        if (!roleOptional.isPresent()) {
            throw new RuntimeException("Role not found with id: " + roleId);
        }
        
        // 2. 检查buttonIds数组中的每个ID是否都存在于permission表中
        if (buttonIds != null && !buttonIds.isEmpty()) {
            List<Permission> existingPermissions = permissionRepository.findAllById(buttonIds);
            if (existingPermissions.size() != buttonIds.size()) {
                throw new RuntimeException("Some permission IDs are invalid");
            }
        }
        
        // 3. 执行全量更新操作
        Role role = roleOptional.get();
        // 清空角色现有的权限
        role.getPermissions().clear();
        
        // 4. 插入新的权限记录
        if (buttonIds != null && !buttonIds.isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(buttonIds);
            role.getPermissions().addAll(permissions);
        }
        
        // 5. 保存角色信息
        roleRepository.save(role);
    }
}