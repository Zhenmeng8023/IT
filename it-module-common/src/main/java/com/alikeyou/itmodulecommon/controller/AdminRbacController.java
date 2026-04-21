package com.alikeyou.itmodulecommon.controller;

import com.alikeyou.itmodulecommon.dto.admin.rbac.AdminRbacMenuDTO;
import com.alikeyou.itmodulecommon.dto.admin.rbac.AdminRbacMenuUpsertRequest;
import com.alikeyou.itmodulecommon.dto.admin.rbac.AdminRbacPermissionDTO;
import com.alikeyou.itmodulecommon.dto.admin.rbac.AdminRbacPermissionUpsertRequest;
import com.alikeyou.itmodulecommon.dto.admin.rbac.AdminRbacRoleDTO;
import com.alikeyou.itmodulecommon.dto.admin.rbac.AdminRbacRoleMenuAssignRequest;
import com.alikeyou.itmodulecommon.dto.admin.rbac.AdminRbacRolePermissionAssignRequest;
import com.alikeyou.itmodulecommon.dto.admin.rbac.AdminRbacRoleUpsertRequest;
import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.entity.Permission;
import com.alikeyou.itmodulecommon.entity.Role;
import com.alikeyou.itmodulecommon.service.MenuService;
import com.alikeyou.itmodulecommon.service.PermissionService;
import com.alikeyou.itmodulecommon.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/rbac")
@PreAuthorize("@authorizationGuard.canManageRbac()")
public class AdminRbacController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private MenuService menuService;

    @GetMapping("/roles/page")
    public ResponseEntity<Page<AdminRbacRoleDTO>> getRolesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Order.desc("id")));
        Page<AdminRbacRoleDTO> rolePage = roleService.getRolesPage(normalizeKeyword(keyword), pageable)
                .map(AdminRbacRoleDTO::from);
        return ResponseEntity.ok(rolePage);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<AdminRbacRoleDTO>> getRoles(@RequestParam(required = false) String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<AdminRbacRoleDTO> roles = roleService.getAllRoles().stream()
                .filter(role -> matchKeyword(role.getRoleName(), role.getDescription(), normalizedKeyword))
                .sorted((left, right) -> Integer.compare(right.getId(), left.getId()))
                .map(AdminRbacRoleDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/roles/{id}")
    public ResponseEntity<AdminRbacRoleDTO> getRoleById(@PathVariable Integer id) {
        return roleService.getRoleById(id)
                .map(role -> ResponseEntity.ok(AdminRbacRoleDTO.from(role)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/roles")
    public ResponseEntity<AdminRbacRoleDTO> createRole(@RequestBody AdminRbacRoleUpsertRequest request) {
        if (!StringUtils.hasText(request.getRoleName())) {
            return ResponseEntity.badRequest().build();
        }
        Role role = new Role();
        role.setRoleName(request.getRoleName().trim());
        role.setDescription(trimToNull(request.getDescription()));
        Role createdRole = roleService.createRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminRbacRoleDTO.from(createdRole));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<AdminRbacRoleDTO> updateRole(@PathVariable Integer id,
                                                       @RequestBody AdminRbacRoleUpsertRequest request) {
        if (!StringUtils.hasText(request.getRoleName())) {
            return ResponseEntity.badRequest().build();
        }
        Role role = new Role();
        role.setRoleName(request.getRoleName().trim());
        role.setDescription(trimToNull(request.getDescription()));
        try {
            Role updatedRole = roleService.updateRole(id, role);
            return ResponseEntity.ok(AdminRbacRoleDTO.from(updatedRole));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Integer id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/roles/{roleId}/menus")
    public ResponseEntity<Void> assignRoleMenus(@PathVariable Integer roleId,
                                                @RequestBody(required = false) AdminRbacRoleMenuAssignRequest request) {
        List<Integer> menuIds = request != null && request.getMenuIds() != null ? request.getMenuIds() : List.of();
        try {
            roleService.assignMenus(roleId, menuIds);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/roles/{roleId}/menus")
    public ResponseEntity<List<AdminRbacMenuDTO>> getRoleMenus(@PathVariable Integer roleId) {
        try {
            List<AdminRbacMenuDTO> menus = roleService.getRoleMenus(roleId).stream()
                    .map(AdminRbacMenuDTO::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(menus);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/roles/{roleId}/permissions")
    public ResponseEntity<Void> assignRolePermissions(@PathVariable Integer roleId,
                                                      @RequestBody(required = false) AdminRbacRolePermissionAssignRequest request) {
        List<Integer> permissionIds = request != null && request.getPermissionIds() != null
                ? request.getPermissionIds()
                : List.of();
        try {
            roleService.assignPermissions(roleId, permissionIds);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<List<AdminRbacPermissionDTO>> getRolePermissions(@PathVariable Integer roleId) {
        try {
            List<AdminRbacPermissionDTO> permissions = roleService.getRolePermissions(roleId).stream()
                    .map(AdminRbacPermissionDTO::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(permissions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/permissions/page")
    public ResponseEntity<Page<AdminRbacPermissionDTO>> getPermissionsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Order.desc("id")));
        Page<AdminRbacPermissionDTO> permissionPage = permissionService.getPermissionsPage(normalizeKeyword(keyword), pageable)
                .map(AdminRbacPermissionDTO::from);
        return ResponseEntity.ok(permissionPage);
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<AdminRbacPermissionDTO>> getPermissions(@RequestParam(required = false) String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<AdminRbacPermissionDTO> permissions = permissionService.getAllPermissions().stream()
                .filter(permission -> matchKeyword(permission.getPermissionCode(), permission.getDescription(), normalizedKeyword))
                .sorted((left, right) -> Integer.compare(right.getId(), left.getId()))
                .map(AdminRbacPermissionDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/permissions/{id}")
    public ResponseEntity<AdminRbacPermissionDTO> getPermissionById(@PathVariable Integer id) {
        return permissionService.getPermissionById(id)
                .map(permission -> ResponseEntity.ok(AdminRbacPermissionDTO.from(permission)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/permissions")
    public ResponseEntity<AdminRbacPermissionDTO> createPermission(@RequestBody AdminRbacPermissionUpsertRequest request) {
        if (!StringUtils.hasText(request.getPermissionCode())) {
            return ResponseEntity.badRequest().build();
        }
        Permission permission = new Permission();
        permission.setPermissionCode(request.getPermissionCode().trim());
        permission.setDescription(trimToNull(request.getDescription()));
        Permission created = permissionService.createPermission(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminRbacPermissionDTO.from(created));
    }

    @PutMapping("/permissions/{id}")
    public ResponseEntity<AdminRbacPermissionDTO> updatePermission(@PathVariable Integer id,
                                                                   @RequestBody AdminRbacPermissionUpsertRequest request) {
        if (!StringUtils.hasText(request.getPermissionCode())) {
            return ResponseEntity.badRequest().build();
        }
        Permission permission = new Permission();
        permission.setPermissionCode(request.getPermissionCode().trim());
        permission.setDescription(trimToNull(request.getDescription()));
        try {
            Permission updated = permissionService.updatePermission(id, permission);
            return ResponseEntity.ok(AdminRbacPermissionDTO.from(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/permissions/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable Integer id) {
        try {
            permissionService.deletePermission(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/menus/page")
    public ResponseEntity<Page<AdminRbacMenuDTO>> getMenusPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer parentId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isHidden,
            @RequestParam(required = false) Integer permissionId) {
        Pageable pageable = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))
        );
        Page<AdminRbacMenuDTO> menuPage = menuService.getMenusPage(
                        normalizeKeyword(keyword),
                        parentId,
                        normalizeType(type),
                        isHidden,
                        permissionId,
                        pageable
                )
                .map(AdminRbacMenuDTO::from);
        return ResponseEntity.ok(menuPage);
    }

    @GetMapping("/menus")
    public ResponseEntity<List<AdminRbacMenuDTO>> getMenus(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer parentId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isHidden,
            @RequestParam(required = false) Integer permissionId) {
        List<AdminRbacMenuDTO> menus = menuService.getMenus(
                        normalizeKeyword(keyword),
                        parentId,
                        normalizeType(type),
                        isHidden,
                        permissionId
                ).stream()
                .map(AdminRbacMenuDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(menus);
    }

    @GetMapping("/menus/{id}")
    public ResponseEntity<AdminRbacMenuDTO> getMenuById(@PathVariable Integer id) {
        return menuService.getMenuById(id)
                .map(menu -> ResponseEntity.ok(AdminRbacMenuDTO.from(menu)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/menus")
    public ResponseEntity<AdminRbacMenuDTO> createMenu(@RequestBody AdminRbacMenuUpsertRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            return ResponseEntity.badRequest().build();
        }
        Menu menu = request.toMenu();
        normalizeMenuRoot(menu);
        Menu created = menuService.createMenu(menu);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminRbacMenuDTO.from(created));
    }

    @PutMapping("/menus/{id}")
    public ResponseEntity<AdminRbacMenuDTO> updateMenu(@PathVariable Integer id,
                                                       @RequestBody AdminRbacMenuUpsertRequest request) {
        Optional<Menu> optionalMenu = menuService.getMenuById(id);
        if (optionalMenu.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Menu existing = optionalMenu.get();
        Menu incoming = request.toMenu();
        Menu merged = mergeMenu(existing, incoming, request);
        normalizeMenuRoot(merged);
        Menu updated = menuService.updateMenu(id, merged);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(AdminRbacMenuDTO.from(updated));
    }

    @DeleteMapping("/menus/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Integer id) {
        if (menuService.getMenuById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }

    private Menu mergeMenu(Menu existing, Menu incoming, AdminRbacMenuUpsertRequest request) {
        boolean hasStructuralUpdate = request.getName() != null
                || request.getPath() != null
                || request.getComponent() != null
                || request.getType() != null
                || request.getIcon() != null
                || request.getParentId() != null
                || request.getPermissionId() != null;

        Menu merged = new Menu();
        merged.setId(existing.getId());
        merged.setName(StringUtils.hasText(incoming.getName()) ? incoming.getName() : existing.getName());
        merged.setIcon(request.getIcon() != null ? incoming.getIcon() : existing.getIcon());
        merged.setSortOrder(incoming.getSortOrder() != null ? incoming.getSortOrder() : existing.getSortOrder());
        merged.setIsHidden(incoming.getIsHidden() != null ? incoming.getIsHidden() : existing.getIsHidden());
        merged.setParentId(hasStructuralUpdate ? incoming.getParentId() : existing.getParentId());
        merged.setPermissionId(hasStructuralUpdate ? incoming.getPermissionId() : existing.getPermissionId());
        merged.setCreatedAt(existing.getCreatedAt());

        String type = normalizeType(request.getType());
        if (type == null) {
            type = existing.getType();
        }

        if ("button".equals(type)) {
            merged.setPath(null);
            merged.setComponent(null);
        } else {
            merged.setPath(request.getPath() != null ? request.getPath() : existing.getPath());
            merged.setComponent(request.getComponent() != null ? request.getComponent() : existing.getComponent());
        }
        merged.setType(type);
        return merged;
    }

    private void normalizeMenuRoot(Menu menu) {
        if (menu.getParentId() != null && menu.getParentId() == 0) {
            menu.setParentId(null);
        }
        if (menu.getIsHidden() == null) {
            menu.setIsHidden(false);
        }
        if (menu.getSortOrder() == null) {
            menu.setSortOrder(0);
        }
    }

    private boolean matchKeyword(String primary, String secondary, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        String first = primary == null ? "" : primary.toLowerCase();
        String second = secondary == null ? "" : secondary.toLowerCase();
        return first.contains(lowerKeyword) || second.contains(lowerKeyword);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 200);
    }

    private String normalizeKeyword(String keyword) {
        return trimToNull(keyword);
    }

    private String normalizeType(String type) {
        String normalized = trimToNull(type);
        if (normalized == null) {
            return null;
        }
        String lower = normalized.toLowerCase();
        if ("menu".equals(lower) || "button".equals(lower)) {
            return lower;
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
