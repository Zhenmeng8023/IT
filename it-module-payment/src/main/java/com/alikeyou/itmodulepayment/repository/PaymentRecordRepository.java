package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    // 根据订单ID查询支付记录
    List<PaymentRecord> findByOrderId(Long orderId);

    // 根据支付平台查询支付记录
    List<PaymentRecord> findByPaymentPlatform(String paymentPlatform);

    // 根据支付状态查询支付记录
    List<PaymentRecord> findByPaymentStatus(String paymentStatus);
}