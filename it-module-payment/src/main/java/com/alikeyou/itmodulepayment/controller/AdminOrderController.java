package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.pojo.Result;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.PaymentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 后台订单管理 Controller
 * 提供订单的增删改查、状态管理等功能
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);
    
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    
    public AdminOrderController(PaymentOrderRepository paymentOrderRepository,
                               PaymentRecordRepository paymentRecordRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }
    
    /**
     * 分页查询所有订单（后台管理）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param status 订单状态（可选）
     * @param type 订单类型（可选）
     * @return 分页订单列表
     */
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> getOrdersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<PaymentOrder> orderPage;
            
            if (status != null && !status.isEmpty() && type != null && !type.isEmpty()) {
                // 按状态和类型筛选
                orderPage = paymentOrderRepository.findByStatusAndType(status, type, pageable);
            } else if (status != null && !status.isEmpty()) {
                // 只按状态筛选
                orderPage = paymentOrderRepository.findByStatus(status, pageable);
            } else if (type != null && !type.isEmpty()) {
                // 只按类型筛选
                orderPage = paymentOrderRepository.findByType(type, pageable);
            } else {
                // 查询所有
                orderPage = paymentOrderRepository.findAll(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orderPage.getContent());
            response.put("total", orderPage.getTotalElements());
            response.put("totalPages", orderPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询订单列表失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 根据ID查询订单详情（包含支付记录）
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        try {
            Optional<PaymentOrder> orderOpt = paymentOrderRepository.findById(id);
            
            if (orderOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "订单不存在");
                return ResponseEntity.status(404).body(error);
            }
            
            PaymentOrder order = orderOpt.get();
            List<PaymentRecord> paymentRecords = paymentRecordRepository.findByOrderId(order.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("order", order);
            response.put("paymentRecords", paymentRecords);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询订单详情失败，订单ID: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 更新订单状态（后台管理）
     */
    @PutMapping("/{id}/status")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        try {
            String newStatus = request.get("status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "状态不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            PaymentOrder order = paymentOrderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));
            
            String oldStatus = order.getStatus();
            order.setStatus(newStatus.toUpperCase());
            order.setUpdatedAt(LocalDateTime.now());
            
            // 如果标记为已支付，设置支付时间
            if ("PAID".equalsIgnoreCase(newStatus) && order.getPayTime() == null) {
                order.setPayTime(LocalDateTime.now());
            }
            
            paymentOrderRepository.save(order);
            
            logger.info("订单状态更新成功，订单ID: {}, 原状态: {}, 新状态: {}", id, oldStatus, newStatus);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "状态更新成功");
            response.put("orderId", id);
            response.put("oldStatus", oldStatus);
            response.put("newStatus", newStatus);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新订单状态失败，订单ID: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 删除订单（后台管理）
     * 注意：会级联删除相关的支付记录
     */
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> deleteOrder(@PathVariable Long id) {
        try {
            PaymentOrder order = paymentOrderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));
            
            // 检查订单状态，已支付的订单需要确认
            if ("PAID".equalsIgnoreCase(order.getStatus())) {
                Map<String, Object> warning = new HashMap<>();
                warning.put("success", false);
                warning.put("message", "该订单已支付，不建议删除。如需删除，请先退款并取消订单。");
                return ResponseEntity.badRequest().body(warning);
            }
            
            paymentOrderRepository.deleteById(id);
            
            logger.info("订单删除成功，订单ID: {}, 订单号: {}", id, order.getOrderNo());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单删除成功");
            response.put("orderId", id);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除订单失败，订单ID: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 统计订单数据（后台仪表盘）
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        try {
            long totalOrders = paymentOrderRepository.count();
            long pendingOrders = paymentOrderRepository.countByStatus("PENDING");
            long paidOrders = paymentOrderRepository.countByStatus("PAID");
            long refundedOrders = paymentOrderRepository.countByStatus("REFUNDED");
            long failedOrders = paymentOrderRepository.countByStatus("FAILED");
            
            // 计算总金额
            List<PaymentOrder> paidOrderList = paymentOrderRepository.findByStatus("PAID");
            double totalAmount = paidOrderList.stream()
                    .mapToDouble(order -> order.getAmount().doubleValue())
                    .sum();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", Map.of(
                "totalOrders", totalOrders,
                "pendingOrders", pendingOrders,
                "paidOrders", paidOrders,
                "refundedOrders", refundedOrders,
                "failedOrders", failedOrders,
                "totalAmount", totalAmount
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取订单统计失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "统计失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 根据用户ID查询订单列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getOrdersByUserId(@PathVariable Long userId) {
        try {
            List<PaymentOrder> orders = paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);
            response.put("count", orders.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询用户订单失败，用户ID: {}", userId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
