package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.RevenueRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RevenueRecordRepository extends JpaRepository<RevenueRecord, Long> {

    // 根据订单ID查询收益记录
    RevenueRecord findByOrderId(Long orderId);

    // 根据来源用户ID查询收益记录
    List<RevenueRecord> findBySourceUserId(Long sourceUserId);

    // 根据结算状态查询收益记录
    List<RevenueRecord> findBySettlementStatus(String settlementStatus);
}