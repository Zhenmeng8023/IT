package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MenuService {

    List<Menu> getAllMenus();

    List<Menu> getMenus(String keyword, Integer parentId, String type, Boolean isHidden, Integer permissionId);

    Optional<Menu> getMenuById(Integer id);

    Menu createMenu(Menu menu);

    Menu createRootMenu(Menu menu);

    List<Menu> getChildrenByParentId(Integer parentId);

    Menu updateMenu(Integer id, Menu menu);

    void deleteMenu(Integer id);

    Page<Menu> getMenusPage(Pageable pageable);

    Page<Menu> getMenusPage(String keyword, Integer parentId, String type, Boolean isHidden, Integer permissionId, Pageable pageable);

    List<Menu> getVisibleMenusByMenuIds(List<Integer> menuIds);

    List<Menu> getVisibleAdminMenus();
}
