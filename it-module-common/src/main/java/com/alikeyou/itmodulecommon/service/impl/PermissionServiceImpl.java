package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.entity.Permission;
import com.alikeyou.itmodulecommon.repository.PermissionRepository;
import com.alikeyou.itmodulecommon.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    public Optional<Permission> getPermissionById(Integer id) {
        return permissionRepository.findById(id);
    }

    @Override
    public Permission createPermission(Permission permission) {
        return permissionRepository.save(permission);
    }

    @Override
    public Permission updatePermission(Integer id, Permission permission) {
        Optional<Permission> existingPermission = permissionRepository.findById(id);
        if (existingPermission.isPresent()) {
            Permission updatedPermission = existingPermission.get();
            updatedPermission.setPermissionCode(permission.getPermissionCode());
            updatedPermission.setDescription(permission.getDescription());
            return permissionRepository.save(updatedPermission);
        } else {
            throw new RuntimeException("Permission not found with id: " + id);
        }
    }

    @Override
    public void deletePermission(Integer id) {
        if (permissionRepository.existsById(id)) {
            permissionRepository.deleteById(id);
        } else {
            throw new RuntimeException("Permission not found with id: " + id);
        }
    }
}