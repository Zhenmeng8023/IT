package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Integer> {

    // 根据父菜单ID获取子菜单
    @Query("SELECT m FROM Menu m WHERE m.parentId = :parentId")
    List<Menu> findByParentId(Integer parentId);

    // 获取所有顶级菜单（parentId为null或0）
    @Query("SELECT m FROM Menu m WHERE m.parentId IS NULL OR m.parentId = 0")
    List<Menu> findRootMenus();
    
    // 根据权限ID列表查询菜单
    @Query("SELECT m FROM Menu m WHERE m.permission.id IN :permissionIds AND m.isHidden = false")
    List<Menu> findByPermissionIds(List<Integer> permissionIds);
}