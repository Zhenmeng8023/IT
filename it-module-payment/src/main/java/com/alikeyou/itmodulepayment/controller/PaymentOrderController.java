package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.PaymentOrderDTO;
import com.alikeyou.itmodulepayment.entity.Membership;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.entity.UserPurchase;
import com.alikeyou.itmodulepayment.repository.*;
import com.alikeyou.itmodulepayment.service.PaymentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("isAuthenticated()")
public class PaymentOrderController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentOrderController.class);

    private final PaymentOrderService paymentOrderService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserInfoRepository userInfoRepository;
    private final MembershipLevelRepository membershipLevelRepository;
    private final MembershipRepository membershipRepository;
    private final PaidContentRepository paidContentRepository;
    private final RevenueRecordRepository revenueRecordRepository;
    private final UserPurchaseRepository userPurchaseRepository;

    public PaymentOrderController(
            PaymentOrderService paymentOrderService,
            PaymentOrderRepository paymentOrderRepository,
            PaymentRecordRepository paymentRecordRepository,
            UserInfoRepository userInfoRepository,
            MembershipLevelRepository membershipLevelRepository,
            MembershipRepository membershipRepository,
            PaidContentRepository paidContentRepository,
            RevenueRecordRepository revenueRecordRepository,
            UserPurchaseRepository userPurchaseRepository) {
        this.paymentOrderService = paymentOrderService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.userInfoRepository = userInfoRepository;
        this.membershipLevelRepository = membershipLevelRepository;
        this.membershipRepository = membershipRepository;
        this.paidContentRepository = paidContentRepository;
        this.revenueRecordRepository = revenueRecordRepository;
        this.userPurchaseRepository = userPurchaseRepository;
    }

    @PostMapping
    @PreAuthorize("@paymentAuthorizationGuard.canAccessUser(#dto.userId)")
    public ResponseEntity<PaymentOrder> createOrder(@RequestBody PaymentOrderDTO dto) {
        PaymentOrder paymentOrder = paymentOrderService.createOrder(dto);
        return new ResponseEntity<>(paymentOrder, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@paymentAuthorizationGuard.canAccessPaymentOrder(#id) && @paymentAuthorizationGuard.canAccessUser(#dto.userId)")
    public ResponseEntity<PaymentOrder> updateOrder(@PathVariable Long id, @RequestBody PaymentOrderDTO dto) {
        PaymentOrder paymentOrder = paymentOrderService.updateOrder(id, dto);
        return new ResponseEntity<>(paymentOrder, HttpStatus.OK);
    }

    @PostMapping("/{id}/payment-url")
    @PreAuthorize("@paymentAuthorizationGuard.canAccessPaymentOrder(#id)")
    public ResponseEntity<Map<String, String>> generatePaymentUrl(@PathVariable Long id, @RequestParam String paymentMethod) {
        PaymentOrder paymentOrder = paymentOrderService.getOrderById(id);
        String paymentUrl = paymentOrderService.generatePaymentUrl(paymentOrder, paymentMethod);
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        response.put("orderNo", paymentOrder.getOrderNo());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@paymentAuthorizationGuard.canAccessPaymentOrder(#id)")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        paymentOrderService.deleteOrder(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@paymentAuthorizationGuard.canAccessPaymentOrder(#id)")
    public ResponseEntity<PaymentOrder> getOrderById(@PathVariable Long id) {
        PaymentOrder paymentOrder = paymentOrderService.getOrderById(id);
        return new ResponseEntity<>(paymentOrder, HttpStatus.OK);
    }

    @GetMapping("/order-no/{orderNo}")
    @PreAuthorize("@paymentAuthorizationGuard.canAccessPaymentOrderNo(#orderNo)")
    public ResponseEntity<PaymentOrder> getOrderByOrderNo(@PathVariable String orderNo) {
        PaymentOrder paymentOrder = paymentOrderService.getOrderByOrderNo(orderNo);
        
        // 【临时修复】如果订单已支付但未更新VIP状态,立即触发同步
        if ("PAID".equals(paymentOrder.getStatus()) && 
            "membership".equals(paymentOrder.getType()) && 
            paymentOrder.getMembershipLevelId() != null) {
            try {
                logger.info("检测到会员订单已支付,检查并同步VIP状态 - 订单号:{}, 用户ID:{}", 
                    orderNo, paymentOrder.getUserId());
                syncMembershipAfterPaid(paymentOrder.getUserId(), paymentOrder.getMembershipLevelId());
                logger.info("VIP状态同步完成 - 用户ID:{}", paymentOrder.getUserId());
            } catch (Exception e) {
                logger.error("VIP状态同步失败 - 订单号:{}, 错误:{}", orderNo, e.getMessage(), e);
                // 不抛出异常,避免影响订单查询
            }
        }
        
        return new ResponseEntity<>(paymentOrder, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@paymentAuthorizationGuard.canAccessUser(#userId)")
    public ResponseEntity<List<PaymentOrder>> getOrdersByUserId(@PathVariable Long userId) {
        return new ResponseEntity<>(paymentOrderService.getOrdersByUserId(userId), HttpStatus.OK);
    }

    @GetMapping("/user/{userId}/type/{type}")
    @PreAuthorize("@paymentAuthorizationGuard.canAccessUser(#userId)")
    public ResponseEntity<List<PaymentOrder>> getOrdersByUserIdAndType(@PathVariable Long userId, @PathVariable String type) {
        return new ResponseEntity<>(paymentOrderService.getOrdersByUserIdAndType(userId, type), HttpStatus.OK);
    }


    @GetMapping("/status/{status}")
    @PreAuthorize("@authorizationGuard.canManageFinance()")
    public ResponseEntity<List<PaymentOrder>> getOrdersByStatus(@PathVariable String status) {
        return new ResponseEntity<>(paymentOrderService.getOrdersByStatus(status), HttpStatus.OK);
    }

    @GetMapping("/paid-content/{paidContentId}")
    @PreAuthorize("@authorizationGuard.canManageFinance()")
    public ResponseEntity<List<PaymentOrder>> getOrdersByPaidContentId(@PathVariable Long paidContentId) {
        return new ResponseEntity<>(paymentOrderService.getOrdersByPaidContentId(paidContentId), HttpStatus.OK);
    }

    @GetMapping("/membership-level/{membershipLevelId}")
    @PreAuthorize("@authorizationGuard.canManageFinance()")
    public ResponseEntity<List<PaymentOrder>> getOrdersByMembershipLevelId(@PathVariable Long membershipLevelId) {
        return new ResponseEntity<>(paymentOrderService.getOrdersByMembershipLevelId(membershipLevelId), HttpStatus.OK);
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("@paymentAuthorizationGuard.canAccessPaymentOrder(#id)")
    public ResponseEntity<String> getOrderStatus(@PathVariable Long id) {
        return ResponseEntity.ok(paymentOrderService.getOrderById(id).getStatus());
    }

    @PostMapping("/pay-test")
    @Transactional
    @PreAuthorize("@authorizationGuard.canManageUsers()")
    public ResponseEntity<Map<String, String>> payTest(@RequestParam String orderNo) {
        try {
            PaymentOrder order = paymentOrderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new RuntimeException("订单不存在，订单号：" + orderNo));

            if ("paid".equalsIgnoreCase(normalize(order.getStatus()))) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "订单已是已支付状态");
                response.put("orderNo", orderNo);
                return ResponseEntity.ok(response);
            }

            order.setStatus("paid");
            order.setPayTime(LocalDateTime.now());
            paymentOrderRepository.save(order);

            List<PaymentRecord> existingRecords = paymentRecordRepository.findByOrderId(order.getId());
            if (existingRecords.isEmpty()) {
                PaymentRecord record = new PaymentRecord();
                record.setOrderId(order.getId());
                record.setPaymentPlatform("TEST-MANUAL");
                record.setTransactionId("TEST_" + System.currentTimeMillis());
                record.setPaymentStatus("success");
                record.setPaymentAmount(order.getAmount());
                record.setPaymentTime(LocalDateTime.now());
                record.setCreatedAt(LocalDateTime.now());
                record.setUpdatedAt(LocalDateTime.now());
                paymentRecordRepository.save(record);
            }

            String type = normalize(order.getType());
            if ("membership".equals(type) && order.getMembershipLevelId() != null) {
                syncMembershipAfterPaid(order.getUserId(), order.getMembershipLevelId());
            } else if ("content".equals(type) && order.getPaidContentId() != null) {
                handleContentPurchaseComplete(order);
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "支付成功");
            response.put("orderNo", orderNo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("测试支付失败，订单号：{}", orderNo, e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "支付失败：" + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private void handleContentPurchaseComplete(PaymentOrder order) {
        var paidContent = paidContentRepository.findById(order.getPaidContentId())
                .orElseThrow(() -> new RuntimeException("付费内容不存在，ID: " + order.getPaidContentId()));

        UserPurchase purchase = userPurchaseRepository.findByUserIdAndPaidContentId(order.getUserId(), order.getPaidContentId())
                .orElseGet(UserPurchase::new);
        purchase.setUserId(order.getUserId());
        purchase.setPaidContentId(order.getPaidContentId());
        purchase.setOrderId(order.getId());
        purchase.setPurchaseTime(order.getPayTime() != null ? order.getPayTime() : LocalDateTime.now());
        purchase.setAccessExpiredAt(null);
        userPurchaseRepository.save(purchase);

        Long authorId = paidContent.getCreatedBy();
        if (authorId == null) {
            return;
        }

        BigDecimal platformFee = order.getAmount().multiply(new BigDecimal("0.2"));
        BigDecimal authorRevenue = order.getAmount().subtract(platformFee);

        var revenueRecord = new com.alikeyou.itmodulepayment.entity.RevenueRecord();
        revenueRecord.setOrderId(order.getId());
        revenueRecord.setSourceUserId(authorId);
        revenueRecord.setPlatformRevenue(platformFee);
        revenueRecord.setAuthorRevenue(authorRevenue);
        revenueRecord.setSettlementStatus("unsettled");
        revenueRecord.setCreatedAt(LocalDateTime.now());
        revenueRecord.setUpdatedAt(LocalDateTime.now());
        revenueRecordRepository.save(revenueRecord);

        UserInfo author = userInfoRepository.findById(authorId).orElse(null);
        if (author != null) {
            BigDecimal currentBalance = author.getBalance() != null ? author.getBalance() : BigDecimal.ZERO;
            author.setBalance(currentBalance.add(authorRevenue));
            userInfoRepository.save(author);
        }
    }

    private void syncMembershipAfterPaid(Long userId, Long membershipLevelId) {
        MembershipLevel level = membershipLevelRepository.findById(membershipLevelId)
                .orElseThrow(() -> new RuntimeException("会员等级不存在，ID: " + membershipLevelId));
        UserInfo user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + userId));

        LocalDateTime now = LocalDateTime.now();
        List<Membership> expiredMemberships = membershipRepository.findByUserIdAndStatusAndEndTimeBefore(userId, "active", now);
        for (Membership expired : expiredMemberships) {
            expired.setStatus("expired");
        }
        if (!expiredMemberships.isEmpty()) {
            membershipRepository.saveAll(expiredMemberships);
        }

        Optional<Membership> activeOpt = membershipRepository.findTopByUserIdAndStatusAndEndTimeAfterOrderByEndTimeDesc(userId, "active", now);
        Membership target = activeOpt.orElseGet(Membership::new);
        if (target.getId() == null) {
            target.setUserId(userId);
            target.setStartTime(now);
            target.setCreatedAt(now);
        }
        LocalDateTime startBase = target.getStartTime() != null ? target.getStartTime() : now;
        LocalDateTime currentEnd = target.getEndTime() != null && target.getEndTime().isAfter(now) ? target.getEndTime() : now;
        LocalDateTime endTime = currentEnd.plusDays(level.getDurationDays());
        target.setStartTime(startBase);
        target.setEndTime(endTime);
        target.setLevelId(level.getId());
        target.setStatus("active");
        target.setUpdatedAt(now);
        membershipRepository.save(target);

        user.setIsPremiumMember(true);
        user.setPremiumExpiryDate(endTime.atZone(ZoneId.systemDefault()).toInstant());
        userInfoRepository.save(user);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
