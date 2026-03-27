package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipLevelRepository extends JpaRepository<MembershipLevel, Long> {

    // 根据状态查询会员等级
    List<MembershipLevel> findByStatus(String status);

    // 根据等级顺序排序查询
    List<MembershipLevel> findAllByOrderByLevelOrderAsc();
}