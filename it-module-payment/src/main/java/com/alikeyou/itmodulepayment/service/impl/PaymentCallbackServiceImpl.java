package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.PaymentRecordRepository;
import com.alikeyou.itmodulepayment.service.PaymentCallbackService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentCallbackServiceImpl(PaymentOrderRepository paymentOrderRepository, PaymentRecordRepository paymentRecordRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Override
    public void handleAlipayCallback(String callbackData) {
        // 解析支付宝回调数据
        // 验证签名
        // 更新支付记录和订单状态
        updatePaymentStatus(callbackData, "alipay");
    }

    @Override
    public void handleWechatCallback(String callbackData) {
        // 解析微信支付回调数据
        // 验证签名
        // 更新支付记录和订单状态
        updatePaymentStatus(callbackData, "wechat");
    }



    private void updatePaymentStatus(String callbackData, String platform) {
        // 解析回调数据获取订单号和支付状态
        // 这里需要根据实际支付平台的回调格式进行解析
        String orderNo = "解析得到的订单号";
        boolean isPaid = true; // 解析得到的支付状态

        if (isPaid) {
            // 更新订单状态
            PaymentOrder order = paymentOrderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));
            order.setStatus("PAID");
            order.setPayTime(LocalDateTime.now());
            paymentOrderRepository.save(order);

            // 更新或创建支付记录
            PaymentRecord record = new PaymentRecord();
            record.setOrderId(order.getId());
            record.setPaymentPlatform(platform);
            record.setPaymentStatus("SUCCESS");
            record.setPaymentTime(LocalDateTime.now());
            record.setCallbackData(callbackData);
            paymentRecordRepository.save(record);
        }
    }
}