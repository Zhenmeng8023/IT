package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.PaymentOrderDTO;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.entity.OrderStatus;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.PaymentRecordRepository;
import com.alikeyou.itmodulepayment.service.PaymentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class PaymentOrderController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentOrderController.class);
    
    private final PaymentOrderService paymentOrderService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserInfoRepository userInfoRepository;
    private final MembershipLevelRepository membershipLevelRepository;

    public PaymentOrderController(
            PaymentOrderService paymentOrderService,
            PaymentOrderRepository paymentOrderRepository,
            PaymentRecordRepository paymentRecordRepository,
            UserInfoRepository userInfoRepository,
            MembershipLevelRepository membershipLevelRepository) {
        this.paymentOrderService = paymentOrderService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.userInfoRepository = userInfoRepository;
        this.membershipLevelRepository = membershipLevelRepository;
    }

    // 创建订单
    @PostMapping
    public ResponseEntity<PaymentOrder> createOrder(@RequestBody PaymentOrderDTO dto) {
        PaymentOrder paymentOrder = paymentOrderService.createOrder(dto);
        return new ResponseEntity<>(paymentOrder, HttpStatus.CREATED);
    }

    // 更新订单
    @PutMapping("/{id}")
    public ResponseEntity<PaymentOrder> updateOrder(@PathVariable Long id, @RequestBody PaymentOrderDTO dto) {
        PaymentOrder paymentOrder = paymentOrderService.updateOrder(id, dto);
        return new ResponseEntity<>(paymentOrder, HttpStatus.OK);
    }

    //生成支付链接
    @PostMapping("/{id}/payment-url")
    public ResponseEntity<Map<String, String>> generatePaymentUrl(
            @PathVariable Long id,
            @RequestParam String paymentMethod) {
        PaymentOrder paymentOrder = paymentOrderService.getOrderById(id);
        String paymentUrl = paymentOrderService.generatePaymentUrl(paymentOrder, paymentMethod);
        
        // 添加详细日志
        System.out.println("========== 生成支付链接 ==========");
        System.out.println("订单号：" + paymentOrder.getOrderNo());
        System.out.println("支付方式：" + paymentMethod);
        System.out.println("生成的支付 URL: " + paymentUrl);
        System.out.println("================================");
        
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        response.put("orderNo", paymentOrder.getOrderNo());
        return ResponseEntity.ok(response);
    }

    // 删除订单
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        paymentOrderService.deleteOrder(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 根据ID查询订单
    @GetMapping("/{id}")
    public ResponseEntity<PaymentOrder> getOrderById(@PathVariable Long id) {
        PaymentOrder paymentOrder = paymentOrderService.getOrderById(id);
        return new ResponseEntity<>(paymentOrder, HttpStatus.OK);
    }

    // 根据订单号查询订单
    @GetMapping("/order-no/{orderNo}")
    public ResponseEntity<PaymentOrder> getOrderByOrderNo(@PathVariable String orderNo) {
        PaymentOrder paymentOrder = paymentOrderService.getOrderByOrderNo(orderNo);
        return new ResponseEntity<>(paymentOrder, HttpStatus.OK);
    }

    // 根据用户ID查询订单
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentOrder>> getOrdersByUserId(@PathVariable Long userId) {
        List<PaymentOrder> paymentOrders = paymentOrderService.getOrdersByUserId(userId);
        return new ResponseEntity<>(paymentOrders, HttpStatus.OK);
    }

    // 根据用户ID和订单类型查询订单
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<PaymentOrder>> getOrdersByUserIdAndType(@PathVariable Long userId, @PathVariable String type) {
        List<PaymentOrder> paymentOrders = paymentOrderService.getOrdersByUserIdAndType(userId, type);
        return new ResponseEntity<>(paymentOrders, HttpStatus.OK);
    }

    // 根据状态查询订单
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentOrder>> getOrdersByStatus(@PathVariable String status) {
        List<PaymentOrder> paymentOrders = paymentOrderService.getOrdersByStatus(status);
        return new ResponseEntity<>(paymentOrders, HttpStatus.OK);
    }

    // 根据付费内容ID查询订单
    @GetMapping("/paid-content/{paidContentId}")
    public ResponseEntity<List<PaymentOrder>> getOrdersByPaidContentId(@PathVariable Long paidContentId) {
        List<PaymentOrder> paymentOrders = paymentOrderService.getOrdersByPaidContentId(paidContentId);
        return new ResponseEntity<>(paymentOrders, HttpStatus.OK);
    }

    // 根据会员等级ID查询订单
    @GetMapping("/membership-level/{membershipLevelId}")
    public ResponseEntity<List<PaymentOrder>> getOrdersByMembershipLevelId(@PathVariable Long membershipLevelId) {
        List<PaymentOrder> paymentOrders = paymentOrderService.getOrdersByMembershipLevelId(membershipLevelId);
        return new ResponseEntity<>(paymentOrders, HttpStatus.OK);
    }

    // 查询订单状态
    @GetMapping("/{id}/status")
    public ResponseEntity<String> getOrderStatus(@PathVariable Long id) {
        PaymentOrder paymentOrder = paymentOrderService.getOrderById(id);
        return ResponseEntity.ok(paymentOrder.getStatus());
    }

    // 测试支付 - 模拟支付成功（完整流程，包含 VIP 状态更新）
    @PostMapping("/pay-test")
    @Transactional
    public ResponseEntity<Map<String, String>> payTest(@RequestParam String orderNo) {
        try {
            logger.info("========== 开始测试支付流程，订单号：{}", orderNo);
            
            // 1. 查询订单
            PaymentOrder order = paymentOrderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new RuntimeException("订单不存在，订单号：" + orderNo));
            
            // 2. 检查订单状态，避免重复处理
            if (OrderStatus.PAID.name().equals(order.getStatus())) {
                logger.info("订单已经是已支付状态，无需重复处理，订单号：{}", orderNo);
                Map<String, String> response = new HashMap<>();
                response.put("message", "订单已是已支付状态");
                response.put("orderNo", orderNo);
                return ResponseEntity.ok(response);
            }
            
            // 3. 更新订单状态为已支付
            order.setStatus(OrderStatus.PAID.name());
            order.setPayTime(LocalDateTime.now());
            paymentOrderRepository.save(order);
            logger.info("订单状态更新为已支付，订单号：{}", orderNo);
            
            // 4. 检查支付记录是否已存在
            List<PaymentRecord> existingRecords = paymentRecordRepository.findByOrderId(order.getId());
            if (!existingRecords.isEmpty()) {
                logger.warn("支付记录已存在，订单号：{}", orderNo);
            } else {
                // 5. 创建支付记录（模拟）
                PaymentRecord record = new PaymentRecord();
                record.setOrderId(order.getId());
                record.setPaymentPlatform("TEST-MANUAL");
                record.setTransactionId("TEST_" + System.currentTimeMillis());
                record.setPaymentStatus("SUCCESS");
                record.setPaymentAmount(order.getAmount());
                record.setPaymentTime(LocalDateTime.now());
                record.setCreatedAt(LocalDateTime.now());
                record.setUpdatedAt(LocalDateTime.now());
                paymentRecordRepository.save(record);
                logger.info("支付记录创建成功，订单号：{}, 支付平台：TEST-MANUAL", orderNo);
            }
            
            // 6. 👉 如果是会员订单，更新用户的 VIP 状态
            if ("membership".equals(order.getType()) && order.getMembershipLevelId() != null) {
                logger.info("检测到会员订单，开始更新 VIP 状态，用户 ID: {}, 会员等级 ID: {}", 
                           order.getUserId(), order.getMembershipLevelId());
                updateUserVipStatus(order.getUserId(), order.getMembershipLevelId());
            } else {
                logger.info("订单类型：{}, 不是会员订单，跳过 VIP 状态更新", order.getType());
            }
            
            logger.info("========== 测试支付流程完成，订单号：{}", orderNo);
            
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
    
    /**
     * 更新用户的 VIP 状态（从 PaymentCallbackServiceImpl 复制的逻辑）
     * @param userId 用户 ID
     * @param membershipLevelId 会员等级 ID
     */
    private void updateUserVipStatus(Long userId, Long membershipLevelId) {
        try {
            // 查询会员等级信息
            MembershipLevel membershipLevel = membershipLevelRepository.findById(membershipLevelId)
                    .orElseThrow(() -> new RuntimeException("会员等级不存在，ID: " + membershipLevelId));
            
            // 查询用户信息
            UserInfo user = userInfoRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + userId));
            
            // 设置 VIP 状态为 true
            user.setIsPremiumMember(true);
            
            // 计算新的过期时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime newExpiryTime;
            
            // 如果用户已有 VIP 状态且未过期，则在原有过期时间上累加
            if (user.getPremiumExpiryDate() != null) {
                Instant expiryInstant = user.getPremiumExpiryDate();
                LocalDateTime currentExpiryTime = LocalDateTime.ofInstant(expiryInstant, ZoneId.systemDefault());
                
                // 如果当前 VIP 已过期，则从现在开始计算
                if (currentExpiryTime.isBefore(now)) {
                    newExpiryTime = now.plusDays(membershipLevel.getDurationDays());
                } else {
                    // 如果当前 VIP 未过期，则累加天数
                    newExpiryTime = currentExpiryTime.plusDays(membershipLevel.getDurationDays());
                }
            } else {
                // 如果没有过期时间，从现在开始计算
                newExpiryTime = now.plusDays(membershipLevel.getDurationDays());
            }
            
            // 设置过期时间
            user.setPremiumExpiryDate(newExpiryTime.atZone(ZoneId.systemDefault()).toInstant());
            
            // 保存用户信息
            userInfoRepository.save(user);
            
            logger.info("✅ 用户 VIP 状态更新成功，用户 ID: {}, 会员等级：{}, 到期时间：{}", 
                       userId, membershipLevel.getName(), newExpiryTime);
            
        } catch (Exception e) {
            logger.error("❌ 更新用户 VIP 状态失败，用户 ID: {}, 会员等级 ID: {}", userId, membershipLevelId, e);
            // 这里不抛出异常，避免影响主流程
        }
    }
    
    // 从实体创建 DTO（保留方法，但不再使用）
    @Deprecated
    private PaymentOrderDTO createDTOFromOrder(PaymentOrder order) {
        PaymentOrderDTO dto = new PaymentOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setUserId(order.getUserId());
        dto.setType(order.getType());
        dto.setTargetId(order.getTargetId());
        dto.setPaidContentId(order.getPaidContentId());
        dto.setMembershipLevelId(order.getMembershipLevelId());
        dto.setAmount(order.getAmount());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setStatus(order.getStatus());
        dto.setPayTime(order.getPayTime());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }
}