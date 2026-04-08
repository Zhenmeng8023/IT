package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.CouponDTO;
import com.alikeyou.itmodulepayment.dto.CouponRedemptionDTO;
import com.alikeyou.itmodulepayment.dto.UserCouponDTO;
import com.alikeyou.itmodulepayment.pojo.Result;
import com.alikeyou.itmodulepayment.service.CouponRedemptionService;
import com.alikeyou.itmodulepayment.service.CouponService;
import com.alikeyou.itmodulepayment.service.UserCouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户优惠券 Controller
 * 提供用户端的优惠券相关接口
 */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private static final Logger logger = LoggerFactory.getLogger(CouponController.class);

    private final CouponService couponService;
    private final UserCouponService userCouponService;
    private final CouponRedemptionService redemptionService;

    public CouponController(CouponService couponService, 
                           UserCouponService userCouponService,
                           CouponRedemptionService redemptionService) {
        this.couponService = couponService;
        this.userCouponService = userCouponService;
        this.redemptionService = redemptionService;
    }

    /**
     * 查询所有可用的优惠券列表
     */
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableCoupons() {
        try {
            List<CouponDTO> coupons = couponService.getEnabledCoupons();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", coupons);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询可用优惠券失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 根据优惠券码查询详情
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Map<String, Object>> getCouponByCode(@PathVariable String code) {
        try {
            CouponDTO coupon = couponService.getCouponByCode(code);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", coupon);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询优惠券失败: code={}", code, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    /**
     * 领取优惠券（通过兑换码）
     */
    @PostMapping("/redeem")
    public ResponseEntity<Map<String, Object>> redeemCoupon(@RequestBody Map<String, String> request) {
        try {
            String couponCode = request.get("couponCode");
            Long userId = Long.parseLong(request.get("userId"));

            if (couponCode == null || couponCode.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "优惠券码不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            UserCouponDTO userCoupon = userCouponService.redeemCoupon(couponCode, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "领取成功");
            response.put("data", userCoupon);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("领取优惠券失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "领取失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    /**
     * 查询用户的优惠券列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserCoupons(@PathVariable Long userId) {
        try {
            List<UserCouponDTO> userCoupons = userCouponService.getUserCoupons(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userCoupons);
            
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
     * 查询用户可用的优惠券
     */
    @GetMapping("/user/{userId}/available")
    public ResponseEntity<Map<String, Object>> getUserAvailableCoupons(@PathVariable Long userId) {
        try {
            List<UserCouponDTO> availableCoupons = userCouponService.getAvailableCoupons(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", availableCoupons);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询用户可用优惠券失败: userId={}", userId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 计算使用优惠券后的金额
     */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateDiscount(@RequestBody Map<String, Object> request) {
        try {
            Long couponId = Long.parseLong(request.get("couponId").toString());
            BigDecimal orderAmount = new BigDecimal(request.get("orderAmount").toString());

            BigDecimal finalAmount = couponService.calculateDiscountAmount(couponId, orderAmount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("originalAmount", orderAmount);
            response.put("finalAmount", finalAmount);
            response.put("discountAmount", orderAmount.subtract(finalAmount));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("计算优惠金额失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    /**
     * 验证优惠券是否可用
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestBody Map<String, Object> request) {
        try {
            Long couponId = Long.parseLong(request.get("couponId").toString());
            BigDecimal orderAmount = new BigDecimal(request.get("orderAmount").toString());

            boolean isValid = couponService.isCouponValid(couponId, orderAmount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", isValid);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("验证优惠券失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "验证失败: " + e.getMessage());
            error.put("valid", false);
            return ResponseEntity.status(400).body(error);
        }
    }

    /**
     * 查询用户的核销记录
     */
    @GetMapping("/user/{userId}/redemptions")
    public ResponseEntity<Map<String, Object>> getUserRedemptions(@PathVariable Long userId) {
        try {
            List<CouponRedemptionDTO> redemptions = redemptionService.getUserRedemptions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", redemptions);
            
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
     * 统计用户优惠总金额
     */
    @GetMapping("/user/{userId}/total-discount")
    public ResponseEntity<Map<String, Object>> getUserTotalDiscount(@PathVariable Long userId) {
        try {
            Double totalDiscount = redemptionService.getUserTotalDiscount(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalDiscount", totalDiscount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("统计用户优惠金额失败: userId={}", userId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "统计失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
