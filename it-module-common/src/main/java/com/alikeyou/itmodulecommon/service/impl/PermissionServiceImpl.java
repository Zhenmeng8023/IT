package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.entity.Permission;
import com.alikeyou.itmodulecommon.repository.PermissionRepository;
import com.alikeyou.itmodulecommon.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    public Page<Permission> getPermissionsPage(String keyword, Pageable pageable) {
        return permissionRepository.searchAdminPermissions(keyword, pageable);
    }

    @Override
    public Optional<Permission> getPermissionById(Integer id) {
        return permissionRepository.findById(id);
    }

    @Override
    public Permission createPermission(Permission permission) {
        permission.setCreatedAt(Instant.now());
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
        }
        throw new RuntimeException("Permission not found with id: " + id);
    }

    @Override
    public void deletePermission(Integer id) {
        if (!permissionRepository.existsById(id)) {
            throw new RuntimeException("Permission not found with id: " + id);
        }
        permissionRepository.deleteById(id);
    }
}
