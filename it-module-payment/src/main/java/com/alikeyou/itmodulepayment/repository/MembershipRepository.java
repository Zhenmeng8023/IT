package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    // 根据用户 ID 查询会员记录
    List<Membership> findByUserId(Long userId);

    // 根据用户 ID 和状态查询会员记录
    List<Membership> findByUserIdAndStatus(Long userId, String status);

    // 查询用户当前有效的会员记录 (status 为 active)
    Optional<Membership> findByUserIdAndStatusOrderByEndTimeDesc(Long userId, String status);
}
