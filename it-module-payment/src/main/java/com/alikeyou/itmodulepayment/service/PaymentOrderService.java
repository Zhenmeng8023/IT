package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.PaymentOrderDTO;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;

import java.util.List;

public interface PaymentOrderService {

    // 创建订单
    PaymentOrder createOrder(PaymentOrderDTO dto);

    // 更新订单
    PaymentOrder updateOrder(Long id, PaymentOrderDTO dto);

    // 删除订单
    void deleteOrder(Long id);

    // 根据ID查询订单
    PaymentOrder getOrderById(Long id);

    // 根据订单号查询订单
    PaymentOrder getOrderByOrderNo(String orderNo);

    // 根据用户ID查询订单
    List<PaymentOrder> getOrdersByUserId(Long userId);

    // 根据用户ID和订单类型查询订单
    List<PaymentOrder> getOrdersByUserIdAndType(Long userId, String type);

    // 根据状态查询订单
    List<PaymentOrder> getOrdersByStatus(String status);

    // 根据付费内容ID查询订单
    List<PaymentOrder> getOrdersByPaidContentId(Long paidContentId);

    // 根据会员等级ID查询订单
    List<PaymentOrder> getOrdersByMembershipLevelId(Long membershipLevelId);
}