package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.repository.MenuRepository;
import com.alikeyou.itmodulecommon.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Override
    public List<Menu> getAllMenus() {
        return menuRepository.findAll();
    }

    @Override
    public List<Menu> getMenus(String keyword, Integer parentId, String type, Boolean isHidden, Integer permissionId) {
        return menuRepository.searchAdminMenus(keyword, parentId, type, isHidden, permissionId);
    }

    @Override
    public Optional<Menu> getMenuById(Integer id) {
        return menuRepository.findById(id);
    }

    @Override
    public Menu createMenu(Menu menu) {
        menu.setCreatedAt(java.time.Instant.now());
        return menuRepository.save(menu);
    }

    @Override
    public Menu createRootMenu(Menu menu) {
        menu.setParentId(null);
        menu.setCreatedAt(java.time.Instant.now());
        return menuRepository.save(menu);
    }

    @Override
    public List<Menu> getChildrenByParentId(Integer parentId) {
        return menuRepository.findByParentId(parentId);
    }

    @Override
    public Menu updateMenu(Integer id, Menu menu) {
        Optional<Menu> existingMenu = menuRepository.findById(id);
        if (existingMenu.isPresent()) {
            Menu updatedMenu = existingMenu.get();
            updatedMenu.setName(menu.getName());
            updatedMenu.setPath(menu.getPath());
            updatedMenu.setComponent(menu.getComponent());
            updatedMenu.setIcon(menu.getIcon());
            updatedMenu.setSortOrder(menu.getSortOrder());
            updatedMenu.setIsHidden(menu.getIsHidden());
            updatedMenu.setParentId(menu.getParentId());
            updatedMenu.setPermissionId(menu.getPermissionId());
            return menuRepository.save(updatedMenu);
        }
        return null;
    }

    @Override
    public void deleteMenu(Integer id) {
        menuRepository.deleteById(id);
    }

    @Override
    public Page<Menu> getMenusPage(Pageable pageable) {
        return menuRepository.findAll(pageable);
    }

    @Override
    public Page<Menu> getMenusPage(String keyword,
                                   Integer parentId,
                                   String type,
                                   Boolean isHidden,
                                   Integer permissionId,
                                   Pageable pageable) {
        return menuRepository.searchAdminMenus(keyword, parentId, type, isHidden, permissionId, pageable);
    }

    @Override
    public List<Menu> getVisibleMenusByMenuIds(List<Integer> menuIds) {
        return menuRepository.findByMenuIds(menuIds);
    }

    @Override
    public List<Menu> getVisibleAdminMenus() {
        return menuRepository.findVisibleAdminMenus();
    }
}
