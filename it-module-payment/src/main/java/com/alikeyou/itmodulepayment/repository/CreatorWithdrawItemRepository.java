package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.CreatorWithdrawItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreatorWithdrawItemRepository extends JpaRepository<CreatorWithdrawItem, Long> {

    // 根据提现请求ID查询提现项目
    List<CreatorWithdrawItem> findByWithdrawRequestId(Long withdrawRequestId);

    // 根据收益记录ID查询提现项目
    CreatorWithdrawItem findByRevenueRecordId(Long revenueRecordId);
}