package com.alikeyou.itmoduleblog.repository;

import com.alikeyou.itmoduleblog.entity.BlogUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogUserRepository extends JpaRepository<BlogUser, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<BlogUser> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<BlogUser> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据状态查找用户
     */
    java.util.List<BlogUser> findByStatus(String status);
}