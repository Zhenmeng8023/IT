package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.CouponDTO;
import com.alikeyou.itmodulepayment.service.CouponService;
import com.alikeyou.itmodulepayment.service.UserCouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 优惠券测试 Controller
 * 用于快速测试优惠券功能
 */
@RestController
@RequestMapping("/api/test/coupons")
@PreAuthorize("@authorizationGuard.canManageFinance()")
public class CouponTestController {

    private static final Logger logger = LoggerFactory.getLogger(CouponTestController.class);

    private final CouponService couponService;
    private final UserCouponService userCouponService;

    public CouponTestController(CouponService couponService, UserCouponService userCouponService) {
        this.couponService = couponService;
        this.userCouponService = userCouponService;
    }

    /**
     * 创建测试优惠券
     */
    @PostMapping("/create-test-coupon")
    public ResponseEntity<Map<String, Object>> createTestCoupon() {
        try {
            CouponDTO couponDTO = new CouponDTO();
            couponDTO.setCode("TEST2026");
            couponDTO.setName("测试优惠券-满100减20");
            couponDTO.setType("AMOUNT_OFF");
            couponDTO.setValue(new BigDecimal("20.00"));
            couponDTO.setMinAmount(new BigDecimal("100.00"));
            couponDTO.setUsageLimitPerUser(5);
            couponDTO.setTotalLimit(1000);
            couponDTO.setStartTime(LocalDateTime.now());
            couponDTO.setEndTime(LocalDateTime.now().plusMonths(3));
            couponDTO.setIsEnabled(true);

            CouponDTO created = couponService.createCoupon(couponDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "测试优惠券创建成功");
            response.put("data", created);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建测试优惠券失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "创建失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 发放测试优惠券给用户
     */
    @PostMapping("/issue-to-user")
    public ResponseEntity<Map<String, Object>> issueToUser(@RequestBody Map<String, Long> request) {
        try {
            Long couponId = request.get("couponId");
            Long userId = request.get("userId");

            if (couponId == null || userId == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "请提供couponId和userId");
                return ResponseEntity.badRequest().body(error);
            }

            var userCoupon = userCouponService.issueCoupon(couponId, userId, "MANUAL", "测试发放");

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
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 查询用户可用优惠券
     */
    @GetMapping("/user/{userId}/available")
    public ResponseEntity<Map<String, Object>> getUserAvailableCoupons(@PathVariable Long userId) {
        try {
            var availableCoupons = userCouponService.getAvailableCoupons(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", availableCoupons.size());
            response.put("data", availableCoupons);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询用户可用优惠券失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
