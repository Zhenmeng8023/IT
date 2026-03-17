package com.alikeyou.itmodulecommon.controller;

import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class MenuController {

    @Getter
    @Setter
    static class MenuRequest {
        private Integer id;
        private String name;
        private String path;
        private String component;
        private String icon;
        private Integer sortOrder;
        private Boolean isHidden;
        private Integer parentId;
        private String type; // "menu" 或 "button"

        public Menu toMenu() {
            Menu menu = new Menu();
            menu.setId(id);
            menu.setName(name);
            menu.setIcon(icon);
            menu.setSortOrder(sortOrder);
            menu.setIsHidden(isHidden);
            menu.setParentId(parentId);

            // 根据菜单类型处理路径
            if ("button".equals(type)) {
                // 若为按钮，前端路由路径和组件路径都置为null
                menu.setPath(null);
                menu.setComponent(null);
            } else if ("menu".equals(type)) {
                // 若为菜单，使用当前的添加逻辑
                menu.setPath(path);
                menu.setComponent(component);
            } else {
                // 默认为菜单类型
                menu.setPath(path);
                menu.setComponent(component);
            }

            return menu;
        }
    }

    @Autowired
    private MenuService menuService;

    // 获取所有菜单
    @Operation(summary = "获取所有菜单", description = "获取所有菜单列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取菜单列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Menu.class)))
    })
    @GetMapping("/menus")
    public ResponseEntity<List<Menu>> getAllMenus() {
        List<Menu> menus = menuService.getAllMenus();
        return ResponseEntity.ok(menus);
    }

    // 根据ID获取菜单详情
    @Operation(summary = "获取菜单详情", description = "根据菜单ID获取菜单详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取菜单详情",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Menu.class))),
            @ApiResponse(responseCode = "404", description = "菜单不存在")
    })
    @GetMapping("/menus/{id}")
    public ResponseEntity<Menu> getMenuById(
            @Parameter(description = "菜单ID", required = true)
            @PathVariable Integer id) {
        Optional<Menu> menu = menuService.getMenuById(id);
        return menu.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 创建菜单
    @Operation(summary = "创建菜单", description = "创建新菜单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功创建菜单",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Menu.class)))
    })
    @PostMapping("/menus")
    public ResponseEntity<Menu> createMenu(
            @Parameter(description = "菜单信息", required = true)
            @RequestBody MenuRequest menuRequest) {
        Menu menu = menuRequest.toMenu();
        Menu createdMenu = menuService.createMenu(menu);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMenu);
    }

    // 创建顶级菜单
    @Operation(summary = "创建顶级菜单", description = "创建一个顶级菜单项")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功创建顶级菜单",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Menu.class)))
    })
    @PostMapping("/menus/root")
    public ResponseEntity<Menu> createRootMenu(
            @Parameter(description = "菜单信息", required = true)
            @RequestBody MenuRequest menuRequest) {
        Menu menu = menuRequest.toMenu();
        Menu createdMenu = menuService.createRootMenu(menu);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMenu);
    }

    // 根据父菜单ID获取子菜单
    @Operation(summary = "获取菜单的子菜单列表", description = "根据父级菜单ID获取其所有子菜单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取子菜单列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Menu.class)))
    })
    @GetMapping("/menus/{parentId}/children")
    public ResponseEntity<List<Menu>> getChildrenByParentId(
            @Parameter(description = "父菜单ID", required = true)
            @PathVariable Integer parentId) {
        List<Menu> children = menuService.getChildrenByParentId(parentId);
        return ResponseEntity.ok(children);
    }

    // 更新菜单
    @Operation(summary = "更新菜单", description = "更新菜单信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功更新菜单",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Menu.class))),
            @ApiResponse(responseCode = "404", description = "菜单不存在")
    })
    @PutMapping("/menus/{id}")
    public ResponseEntity<Menu> updateMenu(
            @Parameter(description = "菜单ID", required = true)
            @PathVariable Integer id,
            @Parameter(description = "菜单信息", required = true)
            @RequestBody Menu menu) {
        Menu updatedMenu = menuService.updateMenu(id, menu);
        if (updatedMenu != null) {
            return ResponseEntity.ok(updatedMenu);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 删除菜单
    @Operation(summary = "删除菜单", description = "删除菜单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功删除菜单")
    })
    @DeleteMapping("/menus/{id}")
    public ResponseEntity<Void> deleteMenu(
            @Parameter(description = "菜单ID", required = true)
            @PathVariable Integer id) {
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }

    // 分页获取菜单列表
    @Operation(summary = "分页获取菜单列表", description = "(后台)分页查询菜单列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取菜单分页列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/admin/menus/page")
    public ResponseEntity<Page<Menu>> getMenusPage(
            @Parameter(description = "页码", required = true)
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", required = true)
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Menu> menusPage = menuService.getMenusPage(pageable);
        return ResponseEntity.ok(menusPage);
    }
}