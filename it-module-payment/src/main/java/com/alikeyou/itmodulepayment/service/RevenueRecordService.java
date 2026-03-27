package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.RevenueRecordDTO;
import com.alikeyou.itmodulepayment.entity.RevenueRecord;

import java.util.List;

public interface RevenueRecordService {

    // 创建收益记录
    RevenueRecord createRevenueRecord(RevenueRecordDTO dto);

    // 更新收益记录
    RevenueRecord updateRevenueRecord(Long id, RevenueRecordDTO dto);

    // 删除收益记录
    void deleteRevenueRecord(Long id);

    // 根据ID查询收益记录
    RevenueRecord getRevenueRecordById(Long id);

    // 根据订单ID查询收益记录
    RevenueRecord getRevenueRecordByOrderId(Long orderId);

    // 根据来源用户ID查询收益记录
    List<RevenueRecord> getRevenueRecordsBySourceUserId(Long sourceUserId);

    // 根据结算状态查询收益记录
    List<RevenueRecord> getRevenueRecordsBySettlementStatus(String settlementStatus);
}
