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

    // 根据来源用户ID和结算状态查询收益记录
    List<RevenueRecord> findBySourceUserIdAndSettlementStatus(Long sourceUserId, String settlementStatus);
    
    // 计算用户的总收益（所有已结算的作者收益总和）
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(r.authorRevenue), 0) FROM RevenueRecord r WHERE r.sourceUserId = :userId AND r.settlementStatus = 'settled'")
    java.math.BigDecimal calculateTotalRevenueByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}