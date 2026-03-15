package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
}
