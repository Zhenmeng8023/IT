package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.PaymentRecordDTO;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;

import java.util.List;

public interface PaymentRecordService {

    // 创建支付记录
    PaymentRecord createPaymentRecord(PaymentRecordDTO dto);

    // 更新支付记录
    PaymentRecord updatePaymentRecord(Long id, PaymentRecordDTO dto);

    // 删除支付记录
    void deletePaymentRecord(Long id);

    // 根据ID查询支付记录
    PaymentRecord getPaymentRecordById(Long id);

    // 根据订单ID查询支付记录
    List<PaymentRecord> getPaymentRecordsByOrderId(Long orderId);

    // 根据支付平台查询支付记录
    List<PaymentRecord> getPaymentRecordsByPaymentPlatform(String paymentPlatform);

    // 根据支付状态查询支付记录
    List<PaymentRecord> getPaymentRecordsByPaymentStatus(String paymentStatus);
}