package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.PaymentOrderDTO;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.service.PaymentOrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentOrderServiceImpl implements PaymentOrderService {

    private final PaymentOrderRepository paymentOrderRepository;

    public PaymentOrderServiceImpl(PaymentOrderRepository paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Override
    public PaymentOrder createOrder(PaymentOrderDTO dto) {
        PaymentOrder paymentOrder = new PaymentOrder();
        BeanUtils.copyProperties(dto, paymentOrder);
        // 生成订单号：ORDER + 时间戳 + 4位随机数
        String orderNo = "ORDER" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (int)(Math.random() * 10000);
        paymentOrder.setOrderNo(orderNo);
        paymentOrder.setCreatedAt(LocalDateTime.now());
        paymentOrder.setUpdatedAt(LocalDateTime.now());
        return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    public PaymentOrder updateOrder(Long id, PaymentOrderDTO dto) {
        PaymentOrder paymentOrder = paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        BeanUtils.copyProperties(dto, paymentOrder);
        paymentOrder.setUpdatedAt(LocalDateTime.now());
        return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    public void deleteOrder(Long id) {
        paymentOrderRepository.deleteById(id);
    }

    @Override
    public PaymentOrder getOrderById(Long id) {
        return paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    @Override
    public PaymentOrder getOrderByOrderNo(String orderNo) {
        return paymentOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    @Override
    public List<PaymentOrder> getOrdersByUserId(Long userId) {
        return paymentOrderRepository.findByUserId(userId);
    }

    @Override
    public List<PaymentOrder> getOrdersByUserIdAndType(Long userId, String type) {
        return paymentOrderRepository.findByUserIdAndType(userId, type);
    }

    @Override
    public List<PaymentOrder> getOrdersByStatus(String status) {
        return paymentOrderRepository.findByStatus(status);
    }

    @Override
    public List<PaymentOrder> getOrdersByPaidContentId(Long paidContentId) {
        return paymentOrderRepository.findByPaidContentId(paidContentId);
    }

    @Override
    public List<PaymentOrder> getOrdersByMembershipLevelId(Long membershipLevelId) {
        return paymentOrderRepository.findByMembershipLevelId(membershipLevelId);
    }
}