package com.alikeyou.itmodulecommon.dto.admin.rbac;

import com.alikeyou.itmodulecommon.entity.Menu;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminRbacMenuDTO {

    private Integer id;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Boolean isHidden;
    private Integer parentId;
    private Integer permissionId;
    private String permissionCode;
    private String type;
    private List<AdminRbacMenuDTO> children = new ArrayList<>();
    private Instant createdAt;

    public static AdminRbacMenuDTO from(Menu menu) {
        AdminRbacMenuDTO dto = new AdminRbacMenuDTO();
        dto.setId(menu.getId());
        dto.setName(menu.getName());
        dto.setPath(menu.getPath());
        dto.setComponent(menu.getComponent());
        dto.setIcon(menu.getIcon());
        dto.setSortOrder(menu.getSortOrder());
        dto.setIsHidden(menu.getIsHidden());
        dto.setParentId(menu.getParentId());
        dto.setPermissionId(menu.getPermissionId());
        dto.setPermissionCode(menu.getPermission() != null ? menu.getPermission().getPermissionCode() : null);
        dto.setType(menu.getType());
        dto.setCreatedAt(menu.getCreatedAt());
        return dto;
    }

    public static List<AdminRbacMenuDTO> toTree(List<Menu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }

        List<Menu> sortedMenus = menus.stream()
                .sorted(menuComparator())
                .toList();
        Map<Integer, AdminRbacMenuDTO> dtoById = new LinkedHashMap<>();
        for (Menu menu : sortedMenus) {
            if (menu.getId() != null) {
                dtoById.put(menu.getId(), AdminRbacMenuDTO.from(menu));
            }
        }

        List<AdminRbacMenuDTO> roots = new ArrayList<>();
        for (AdminRbacMenuDTO dto : dtoById.values()) {
            AdminRbacMenuDTO parent = dto.getParentId() == null ? null : dtoById.get(dto.getParentId());
            if (parent == null) {
                roots.add(dto);
            } else {
                parent.getChildren().add(dto);
            }
        }
        return roots;
    }

    private static Comparator<Menu> menuComparator() {
        return Comparator
                .comparing((Menu menu) -> menu.getSortOrder() == null ? Integer.MAX_VALUE : menu.getSortOrder())
                .thenComparing(menu -> menu.getId() == null ? Integer.MAX_VALUE : menu.getId());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsHidden() {
        return isHidden;
    }

    public void setIsHidden(Boolean hidden) {
        isHidden = hidden;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<AdminRbacMenuDTO> getChildren() {
        return children;
    }

    public void setChildren(List<AdminRbacMenuDTO> children) {
        this.children = children == null ? new ArrayList<>() : children;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
