package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.CouponDTO;
import com.alikeyou.itmodulepayment.dto.CouponRedemptionDTO;
import com.alikeyou.itmodulepayment.dto.UserCouponDTO;
import com.alikeyou.itmodulepayment.service.CouponRedemptionService;
import com.alikeyou.itmodulepayment.service.CouponService;
import com.alikeyou.itmodulepayment.service.UserCouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台优惠券管理 Controller
 * 提供管理员对优惠券的管理功能
 */
@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("@authorizationGuard.canManageFinance()")
public class AdminCouponController {

    private static final Logger logger = LoggerFactory.getLogger(AdminCouponController.class);

    private final CouponService couponService;
    private final UserCouponService userCouponService;
    private final CouponRedemptionService redemptionService;

    public AdminCouponController(CouponService couponService,
                                 UserCouponService userCouponService,
                                 CouponRedemptionService redemptionService) {
        this.couponService = couponService;
        this.userCouponService = userCouponService;
        this.redemptionService = redemptionService;
    }

    // ==================== 优惠券模板管理 ====================

    /**
     * 创建优惠券
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCoupon(@RequestBody CouponDTO couponDTO) {
        try {
            CouponDTO created = couponService.createCoupon(couponDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "创建成功");
            response.put("data", created);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建优惠券失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "创建失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    /**
     * 更新优惠券
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCoupon(@PathVariable Long id, @RequestBody CouponDTO couponDTO) {
        try {
            CouponDTO updated = couponService.updateCoupon(id, couponDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "更新成功");
            response.put("data", updated);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新优惠券失败: id={}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    /**
     * 删除优惠券
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCoupon(@PathVariable Long id) {
        try {
            couponService.deleteCoupon(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除优惠券失败: id={}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    /**
     * 查询优惠券详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCouponDetail(@PathVariable Long id) {
        try {
            CouponDTO coupon = couponService.getCouponById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", coupon);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询优惠券详情失败: id={}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    /**
     * 分页查询所有优惠券
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<CouponDTO> couponPage = couponService.getAllCoupons(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", couponPage.getContent());
            response.put("total", couponPage.getTotalElements());
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", couponPage.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询优惠券列表失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 启用/禁用优惠券
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> toggleCouponStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "请提供enabled参数");
                return ResponseEntity.badRequest().body(error);
            }

            CouponDTO updated = couponService.toggleCouponStatus(id, enabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", enabled ? "启用成功" : "禁用成功");
            response.put("data", updated);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("切换优惠券状态失败: id={}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    // ==================== 用户优惠券管理 ====================

    /**
     * 发放优惠券给用户
     */
    @PostMapping("/issue")
    public ResponseEntity<Map<String, Object>> issueCoupon(@RequestBody Map<String, Object> request) {
        try {
            Long couponId = Long.parseLong(request.get("couponId").toString());
            Long userId = Long.parseLong(request.get("userId").toString());
            String sourceType = (String) request.getOrDefault("sourceType", "MANUAL");
            String remark = (String) request.get("remark");

            UserCouponDTO userCoupon = userCouponService.issueCoupon(couponId, userId, sourceType, remark);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "发放成功");
            response.put("data", userCoupon);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("发放优惠券失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "发放失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    /**
     * 批量发放优惠券
     */
    @PostMapping("/batch-issue")
    public ResponseEntity<Map<String, Object>> batchIssueCoupons(@RequestBody Map<String, Object> request) {
        try {
            Long couponId = Long.parseLong(request.get("couponId").toString());
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) request.get("userIds");
            String sourceType = (String) request.getOrDefault("sourceType", "MANUAL");
            String remark = (String) request.get("remark");

            if (userIds == null || userIds.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "用户ID列表不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            List<UserCouponDTO> results = userCouponService.batchIssueCoupons(couponId, userIds, sourceType, remark);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量发放完成");
            response.put("data", results);
            response.put("successCount", results.size());
            response.put("totalCount", userIds.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("批量发放优惠券失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "批量发放失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    /**
     * 查询用户的优惠券
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserCoupons(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<UserCouponDTO> userCouponPage = userCouponService.getUserCouponsPaged(userId, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userCouponPage.getContent());
            response.put("total", userCouponPage.getTotalElements());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询用户优惠券失败: userId={}", userId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 作废用户优惠券
     */
    @PutMapping("/user-coupon/{userCouponId}/void")
    public ResponseEntity<Map<String, Object>> voidUserCoupon(
            @PathVariable Long userCouponId,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            userCouponService.voidCoupon(userCouponId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "作废成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("作废优惠券失败: userCouponId={}", userCouponId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "作废失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    // ==================== 核销记录管理 ====================

    /**
     * 查询用户的核销记录
     */
    @GetMapping("/redemptions/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRedemptions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<CouponRedemptionDTO> redemptionPage = redemptionService.getUserRedemptionsPaged(userId, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", redemptionPage.getContent());
            response.put("total", redemptionPage.getTotalElements());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询用户核销记录失败: userId={}", userId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 回滚核销记录
     */
    @PutMapping("/redemption/{redemptionId}/rollback")
    public ResponseEntity<Map<String, Object>> rollbackRedemption(
            @PathVariable Long redemptionId,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            CouponRedemptionDTO updated = redemptionService.rollbackRedemption(redemptionId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "回滚成功");
            response.put("data", updated);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("回滚核销记录失败: redemptionId={}", redemptionId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "回滚失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }
}
