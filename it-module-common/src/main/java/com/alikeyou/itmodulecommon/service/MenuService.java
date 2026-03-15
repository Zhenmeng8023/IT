package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MenuService {

    // 获取所有菜单
    List<Menu> getAllMenus();

    // 根据ID获取菜单详情
    Optional<Menu> getMenuById(Integer id);

    // 创建菜单
    Menu createMenu(Menu menu);

    // 创建顶级菜单
    Menu createRootMenu(Menu menu);

    // 根据父菜单ID获取子菜单
    List<Menu> getChildrenByParentId(Integer parentId);

    // 更新菜单
    Menu updateMenu(Integer id, Menu menu);

    // 删除菜单
    void deleteMenu(Integer id);

    // 分页获取菜单列表
    Page<Menu> getMenusPage(Pageable pageable);

    // 根据权限ID列表获取可见的菜单
    List<Menu> getVisibleMenusByPermissions(List<Integer> permissionIds);
}
