package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    // 根据订单号查询订单
    Optional<PaymentOrder> findByOrderNo(String orderNo);

    // 根据用户ID查询订单
    List<PaymentOrder> findByUserId(Long userId);

    // 根据用户ID和订单类型查询订单
    List<PaymentOrder> findByUserIdAndType(Long userId, String type);

    // 根据状态查询订单
    List<PaymentOrder> findByStatus(String status);

    // 根据付费内容ID查询订单
    List<PaymentOrder> findByPaidContentId(Long paidContentId);

    // 根据会员等级ID查询订单
    List<PaymentOrder> findByMembershipLevelId(Long membershipLevelId);
}