package com.alikeyou.itmoduleuser.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alikeyou.itmodulecommon.entity.UserInfo;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    // 根据用户名查找用户
    Optional<UserInfo> findByUsername(String username);

    // 根据邮箱查找用户
    Optional<UserInfo> findByEmail(String email);

    // 根据手机号查找用户
    Optional<UserInfo> findByPhone(String phone);
}