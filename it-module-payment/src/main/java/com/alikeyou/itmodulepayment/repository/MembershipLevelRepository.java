package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipLevelRepository extends JpaRepository<MembershipLevel, Long> {

    // 根据是否启用查询会员等级
    List<MembershipLevel> findByIsEnabled(Boolean isEnabled);
}