package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.CreatorSettlementAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreatorSettlementAccountRepository extends JpaRepository<CreatorSettlementAccount, Long> {

    // 根据用户ID查询结算账户
    List<CreatorSettlementAccount> findByUserId(Long userId);

    // 根据用户ID和状态查询结算账户
    List<CreatorSettlementAccount> findByUserIdAndStatus(Long userId, String status);
}