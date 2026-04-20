package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    @Query("""
            SELECT p FROM Permission p
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(p.permissionCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR COALESCE(p.description, '') LIKE CONCAT('%', :keyword, '%'))
            ORDER BY p.id DESC
            """)
    Page<Permission> searchAdminPermissions(@Param("keyword") String keyword, Pageable pageable);
}
