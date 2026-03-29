package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.PaymentOrderDTO;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.service.PaymentOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    public PaymentOrderController(PaymentOrderService paymentOrderService) {
        this.paymentOrderService = paymentOrderService;
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

    // 测试支付 - 模拟支付成功
    @PostMapping("/pay-test")
    public ResponseEntity<Map<String, String>> payTest(@RequestParam String orderNo) {
        try {
            PaymentOrder paymentOrder = paymentOrderService.getOrderByOrderNo(orderNo);
            
            // 直接更新订单状态，而不是通过 DTO
            paymentOrder.setStatus("PAID");
            paymentOrder.setPayTime(java.time.LocalDateTime.now());
            PaymentOrder updatedOrder = paymentOrderService.updateOrder(paymentOrder.getId(), createDTOFromOrder(paymentOrder));
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "支付成功");
            response.put("orderNo", orderNo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "支付失败：" + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // 从实体创建 DTO
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