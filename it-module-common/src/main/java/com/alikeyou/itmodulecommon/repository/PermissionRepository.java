package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
}