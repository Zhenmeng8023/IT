package com.alikeyou.itmodulecommon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alikeyou.itmodulecommon.entity.UserInfo;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    // 根据用户名查找用户
    @Query("SELECT u FROM UserInfo u LEFT JOIN FETCH u.region LEFT JOIN FETCH u.authorTag WHERE u.username = :username")
    Optional<UserInfo> findByUsername(String username);

    // 根据邮箱查找用户
    @Query("SELECT u FROM UserInfo u LEFT JOIN FETCH u.region LEFT JOIN FETCH u.authorTag WHERE u.email = :email")
    Optional<UserInfo> findByEmail(String email);

    // 根据手机号查找用户
    @Query("SELECT u FROM UserInfo u LEFT JOIN FETCH u.region LEFT JOIN FETCH u.authorTag WHERE u.phone = :phone")
    Optional<UserInfo> findByPhone(String phone);
    
    // 根据ID查找用户，加载关联信息
    @Query("SELECT u FROM UserInfo u LEFT JOIN FETCH u.region LEFT JOIN FETCH u.authorTag WHERE u.id = :id")
    Optional<UserInfo> findByIdWithAssociations(Long id);

    // 查找所有用户，加载关联信息
    @Query("SELECT u FROM UserInfo u LEFT JOIN FETCH u.region LEFT JOIN FETCH u.authorTag")
    List<UserInfo> findAllWithAssociations();

    List<UserInfo> findByRoleIdIn(List<Integer> roleIds);

    @Query("""
            SELECT u FROM UserInfo u
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY u.id DESC
            """)
    List<UserInfo> searchForNotification(@Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);
}
