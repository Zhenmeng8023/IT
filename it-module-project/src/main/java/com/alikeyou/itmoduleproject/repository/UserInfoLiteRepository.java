package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserInfoLiteRepository extends JpaRepository<UserInfoLite, Long> {

    List<UserInfoLite> findByIdIn(Collection<Long> ids);
}
