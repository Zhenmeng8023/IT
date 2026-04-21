package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    @Query("""
            SELECT DISTINCT r FROM Role r
            LEFT JOIN FETCH r.menus m
            LEFT JOIN FETCH m.permission
            WHERE r.id = :id
            """)
    Optional<Role> findWithMenusAndPermissionsById(@Param("id") Integer id);

    @Query("""
            SELECT r FROM Role r
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(r.roleName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR COALESCE(r.description, '') LIKE CONCAT('%', :keyword, '%'))
            ORDER BY r.id DESC
            """)
    Page<Role> searchAdminRoles(@Param("keyword") String keyword, Pageable pageable);
}
