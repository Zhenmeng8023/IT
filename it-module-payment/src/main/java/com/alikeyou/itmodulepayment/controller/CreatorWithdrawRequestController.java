package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.CreatorWithdrawRequestDTO;
import com.alikeyou.itmodulepayment.entity.CreatorWithdrawRequest;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.repository.PaymentRecordRepository;
import com.alikeyou.itmodulepayment.service.CreatorWithdrawRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creator-withdraw-requests")
public class CreatorWithdrawRequestController {

    private static final Logger logger = LoggerFactory.getLogger(CreatorWithdrawRequestController.class);

    private final CreatorWithdrawRequestService creatorWithdrawRequestService;
    private final PaymentRecordRepository paymentRecordRepository;

    public CreatorWithdrawRequestController(CreatorWithdrawRequestService creatorWithdrawRequestService,
                                           PaymentRecordRepository paymentRecordRepository) {
        this.creatorWithdrawRequestService = creatorWithdrawRequestService;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    /**
     * 提交提现申请
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> submitWithdrawRequest(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Long settlementAccountId = Long.valueOf(params.get("settlementAccountId").toString());
            BigDecimal withdrawAmount = new BigDecimal(params.get("withdrawAmount").toString());
            String remark = params.get("remark") != null ? params.get("remark").toString() : "";

            CreatorWithdrawRequest request = creatorWithdrawRequestService.submitWithdrawRequest(
                    userId, settlementAccountId, withdrawAmount, remark
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "提现申请提交成功");
            result.put("data", request);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("提现参数错误: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            logger.error("提现申请失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取用户可提现余额
     */
    @GetMapping("/user/{userId}/available-balance")
    public ResponseEntity<Map<String, Object>> getAvailableBalance(@PathVariable Long userId) {
        try {
            BigDecimal balance = creatorWithdrawRequestService.getAvailableWithdrawBalance(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", balance);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取可提现余额失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取余额失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 审核通过提现申请（管理员）
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveWithdrawRequest(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> params) {
        try {
            Long reviewedBy = Long.valueOf(params.get("reviewedBy").toString());
            String reviewNote = params.get("reviewNote") != null ? params.get("reviewNote").toString() : "";

            CreatorWithdrawRequest request = creatorWithdrawRequestService.approveWithdrawRequest(
                    id, reviewedBy, reviewNote
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "审核通过");
            result.put("data", request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("审核失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 拒绝提现申请（管理员）
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectWithdrawRequest(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> params) {
        try {
            Long reviewedBy = Long.valueOf(params.get("reviewedBy").toString());
            String reviewNote = params.get("reviewNote") != null ? params.get("reviewNote").toString() : "";

            CreatorWithdrawRequest request = creatorWithdrawRequestService.rejectWithdrawRequest(
                    id, reviewedBy, reviewNote
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "已拒绝");
            result.put("data", request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("拒绝失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 执行打款（管理员）
     */
    @PutMapping("/{id}/pay")
    public ResponseEntity<Map<String, Object>> processPayment(@PathVariable Long id) {
        try {
            CreatorWithdrawRequest request = creatorWithdrawRequestService.processWithdrawPayment(id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "打款成功");
            result.put("data", request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("打款失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // 根据ID查询创作者提现请求
    @GetMapping("/{id}")
    public ResponseEntity<CreatorWithdrawRequest> getCreatorWithdrawRequestById(@PathVariable Long id) {
        CreatorWithdrawRequest request = creatorWithdrawRequestService.getCreatorWithdrawRequestById(id);
        return new ResponseEntity<>(request, HttpStatus.OK);
    }

    // 根据用户ID查询创作者提现请求
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CreatorWithdrawRequest>> getCreatorWithdrawRequestsByUserId(@PathVariable Long userId) {
        List<CreatorWithdrawRequest> requests = creatorWithdrawRequestService.getCreatorWithdrawRequestsByUserId(userId);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }

    // 根据状态查询创作者提现请求
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CreatorWithdrawRequest>> getCreatorWithdrawRequestsByStatus(@PathVariable String status) {
        List<CreatorWithdrawRequest> requests = creatorWithdrawRequestService.getCreatorWithdrawRequestsByStatus(status);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }

    // 根据用户ID和状态查询创作者提现请求
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<CreatorWithdrawRequest>> getCreatorWithdrawRequestsByUserIdAndStatus(
            @PathVariable Long userId, @PathVariable String status) {
        List<CreatorWithdrawRequest> requests = creatorWithdrawRequestService.getCreatorWithdrawRequestsByUserIdAndStatus(userId, status);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }
    
    /**
     * 查询订单的支付记录
     */
    @GetMapping("/payment-records/order/{orderId}")
    public ResponseEntity<List<PaymentRecord>> getPaymentRecordsByOrderId(@PathVariable Long orderId) {
        List<PaymentRecord> records = paymentRecordRepository.findByOrderId(orderId);
        return new ResponseEntity<>(records, HttpStatus.OK);
    }
}