package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.RevenueRecordDTO;
import com.alikeyou.itmodulepayment.entity.RevenueRecord;
import com.alikeyou.itmodulepayment.repository.RevenueRecordRepository;
import com.alikeyou.itmodulepayment.service.RevenueRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RevenueRecordServiceImpl implements RevenueRecordService {

    private final RevenueRecordRepository revenueRecordRepository;

    public RevenueRecordServiceImpl(RevenueRecordRepository revenueRecordRepository) {
        this.revenueRecordRepository = revenueRecordRepository;
    }

    @Override
    public RevenueRecord createRevenueRecord(RevenueRecordDTO dto) {
        RevenueRecord revenueRecord = new RevenueRecord();
        BeanUtils.copyProperties(dto, revenueRecord);
        revenueRecord.setCreatedAt(LocalDateTime.now());
        revenueRecord.setUpdatedAt(LocalDateTime.now());
        return revenueRecordRepository.save(revenueRecord);
    }

    @Override
    public RevenueRecord updateRevenueRecord(Long id, RevenueRecordDTO dto) {
        RevenueRecord revenueRecord = revenueRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("收益记录不存在"));
        BeanUtils.copyProperties(dto, revenueRecord);
        revenueRecord.setUpdatedAt(LocalDateTime.now());
        return revenueRecordRepository.save(revenueRecord);
    }

    @Override
    public void deleteRevenueRecord(Long id) {
        revenueRecordRepository.deleteById(id);
    }

    @Override
    public RevenueRecord getRevenueRecordById(Long id) {
        return revenueRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("收益记录不存在"));
    }

    @Override
    public RevenueRecord getRevenueRecordByOrderId(Long orderId) {
        return revenueRecordRepository.findByOrderId(orderId);
    }

    @Override
    public List<RevenueRecord> getRevenueRecordsBySourceUserId(Long sourceUserId) {
        return revenueRecordRepository.findBySourceUserId(sourceUserId);
    }

    @Override
    public List<RevenueRecord> getRevenueRecordsBySettlementStatus(String settlementStatus) {
        return revenueRecordRepository.findBySettlementStatus(settlementStatus);
    }
    
    @Override
    public java.math.BigDecimal calculateTotalRevenueByUserId(Long userId) {
        return revenueRecordRepository.calculateTotalRevenueByUserId(userId);
    }
}