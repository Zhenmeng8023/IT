package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    // 根据订单号查询订单
    Optional<PaymentOrder> findByOrderNo(String orderNo);

    // 根据用户 ID 查询订单
    List<PaymentOrder> findByUserId(Long userId);

    // 根据用户 ID 和订单类型查询订单
    List<PaymentOrder> findByUserIdAndType(Long userId, String type);

    // 根据状态查询订单
    List<PaymentOrder> findByStatus(String status);

    // 根据付费内容 ID 查询订单
    List<PaymentOrder> findByPaidContentId(Long paidContentId);

    // 根据会员等级 ID 查询订单
    List<PaymentOrder> findByMembershipLevelId(Long membershipLevelId);

    // 检查用户是否已购买某付费内容（根据用户 ID、付费内容 ID 和状态查询）
    boolean existsByUserIdAndPaidContentIdAndStatus(Long userId, Long paidContentId, String status);
    
    // 后台管理专用方法
    Page<PaymentOrder> findAll(Pageable pageable);
    Page<PaymentOrder> findByStatus(String status, Pageable pageable);
    Page<PaymentOrder> findByType(String type, Pageable pageable);
    Page<PaymentOrder> findByStatusAndType(String status, String type, Pageable pageable);
    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByStatus(String status);
}
