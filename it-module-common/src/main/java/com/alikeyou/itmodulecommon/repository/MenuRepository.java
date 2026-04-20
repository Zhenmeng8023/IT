package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Integer> {

    @Query("SELECT m FROM Menu m WHERE m.parentId = :parentId")
    List<Menu> findByParentId(Integer parentId);

    @Query("SELECT m FROM Menu m WHERE m.parentId IS NULL OR m.parentId = 0")
    List<Menu> findRootMenus();

    @Query("SELECT m FROM Menu m WHERE m.id IN :menuIds AND m.isHidden = false")
    List<Menu> findByMenuIds(List<Integer> menuIds);

    @Query("""
            SELECT m FROM Menu m LEFT JOIN FETCH m.permission
            WHERE m.isHidden = false
              AND m.path LIKE '/admin%'
              AND (m.path IS NOT NULL OR m.component IS NOT NULL)
            ORDER BY m.sortOrder ASC, m.id ASC
            """)
    List<Menu> findVisibleAdminMenus();

    @Query("""
            SELECT m FROM Menu m
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(m.path, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(m.component, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:parentId IS NULL OR m.parentId = :parentId)
              AND (:isHidden IS NULL OR m.isHidden = :isHidden)
              AND (:permissionId IS NULL OR m.permissionId = :permissionId)
              AND (:type IS NULL OR :type = ''
                   OR (:type = 'button' AND m.path IS NULL AND m.component IS NULL)
                   OR (:type = 'menu' AND (m.path IS NOT NULL OR m.component IS NOT NULL)))
            ORDER BY m.sortOrder ASC, m.id ASC
            """)
    Page<Menu> searchAdminMenus(@Param("keyword") String keyword,
                                @Param("parentId") Integer parentId,
                                @Param("type") String type,
                                @Param("isHidden") Boolean isHidden,
                                @Param("permissionId") Integer permissionId,
                                Pageable pageable);

    @Query("""
            SELECT m FROM Menu m
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(m.path, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(m.component, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:parentId IS NULL OR m.parentId = :parentId)
              AND (:isHidden IS NULL OR m.isHidden = :isHidden)
              AND (:permissionId IS NULL OR m.permissionId = :permissionId)
              AND (:type IS NULL OR :type = ''
                   OR (:type = 'button' AND m.path IS NULL AND m.component IS NULL)
                   OR (:type = 'menu' AND (m.path IS NOT NULL OR m.component IS NOT NULL)))
            ORDER BY m.sortOrder ASC, m.id ASC
            """)
    List<Menu> searchAdminMenus(@Param("keyword") String keyword,
                                @Param("parentId") Integer parentId,
                                @Param("type") String type,
                                @Param("isHidden") Boolean isHidden,
                                @Param("permissionId") Integer permissionId);

    @Query("SELECT m FROM Menu m WHERE m.permissionId IN :permissionIds")
    List<Menu> findByPermissionIdIn(@Param("permissionIds") List<Integer> permissionIds);
}
