package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.PaymentRecordDTO;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.repository.PaymentRecordRepository;
import com.alikeyou.itmodulepayment.service.PaymentRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentRecordServiceImpl implements PaymentRecordService {

    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentRecordServiceImpl(PaymentRecordRepository paymentRecordRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Override
    public PaymentRecord createPaymentRecord(PaymentRecordDTO dto) {
        PaymentRecord paymentRecord = new PaymentRecord();
        BeanUtils.copyProperties(dto, paymentRecord);
        paymentRecord.setCreatedAt(LocalDateTime.now());
        paymentRecord.setUpdatedAt(LocalDateTime.now());
        return paymentRecordRepository.save(paymentRecord);
    }

    @Override
    public PaymentRecord updatePaymentRecord(Long id, PaymentRecordDTO dto) {
        PaymentRecord paymentRecord = paymentRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));
        BeanUtils.copyProperties(dto, paymentRecord);
        paymentRecord.setUpdatedAt(LocalDateTime.now());
        return paymentRecordRepository.save(paymentRecord);
    }

    @Override
    public void deletePaymentRecord(Long id) {
        paymentRecordRepository.deleteById(id);
    }

    @Override
    public PaymentRecord getPaymentRecordById(Long id) {
        return paymentRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));
    }

    @Override
    public List<PaymentRecord> getPaymentRecordsByOrderId(Long orderId) {
        return paymentRecordRepository.findByOrderId(orderId);
    }

    @Override
    public List<PaymentRecord> getPaymentRecordsByPaymentPlatform(String paymentPlatform) {
        return paymentRecordRepository.findByPaymentPlatform(paymentPlatform);
    }

    @Override
    public List<PaymentRecord> getPaymentRecordsByPaymentStatus(String paymentStatus) {
        return paymentRecordRepository.findByPaymentStatus(paymentStatus);
    }
}