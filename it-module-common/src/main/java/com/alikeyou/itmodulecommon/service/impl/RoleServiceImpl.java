package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.entity.Role;
import com.alikeyou.itmodulecommon.repository.MenuRepository;
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
    public void assignMenus(Integer roleId, List<Integer> menuIds) {
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            List<Menu> menus = menuRepository.findAllById(menuIds);
            role.getMenus().clear();
            role.getMenus().addAll(menus);
            roleRepository.save(role);
        } else {
            throw new RuntimeException("Role not found with id: " + roleId);
        }
    }

    @Override
    public List<Menu> getRoleMenus(Integer roleId) {
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            return role.getMenus().stream().collect(java.util.stream.Collectors.toList());
        } else {
            throw new RuntimeException("Role not found with id: " + roleId);
        }
    }
}