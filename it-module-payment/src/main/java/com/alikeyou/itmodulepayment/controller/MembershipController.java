package com.alikeyou.itmodulepayment.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.MembershipDTO;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.entity.OrderStatus;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.pojo.Result;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.service.MembershipService;
import com.alikeyou.itmodulepayment.service.UserCouponService;
import com.alikeyou.itmodulepayment.util.PayUtil;
import com.alikeyou.itmodulepayment.entity.UserCoupon;
import com.alikeyou.itmodulepayment.entity.Coupon;
import com.alikeyou.itmodulepayment.repository.UserCouponRepository;
import com.alikeyou.itmodulepayment.repository.CouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {
    private static final Logger logger = LoggerFactory.getLogger(MembershipController.class);

    private final MembershipService membershipService;
    
    @Autowired
    private PaymentOrderRepository paymentOrderRepository;
    
    @Autowired
    private MembershipLevelRepository membershipLevelRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private PayUtil payUtil;
    
    @Autowired
    private UserCouponService userCouponService;
    
    @Autowired
    private UserCouponRepository userCouponRepository;
    
    @Autowired
    private CouponRepository couponRepository;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    // 检查用户是否为会员
    @GetMapping("/check-vip/{userId}")
    public ResponseEntity<Map<String, Object>> checkUserIsVip(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        Boolean isVip = membershipService.checkUserIsVip(userId);
        response.put("isVip", isVip);
        response.put("userId", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 获取用户的会员信息
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserMembershipInfo(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        var membershipOpt = membershipService.getUserMembershipInfo(userId);
        
        if (membershipOpt.isPresent()) {
            MembershipDTO dto = membershipOpt.get();
            response.put("success", true);
            response.put("data", dto);
        } else {
            response.put("success", false);
            response.put("message", "未找到用户会员信息");
        }
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 获取用户当前有效的会员信息
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<Map<String, Object>> getUserActiveMembership(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        var membershipOpt = membershipService.getUserActiveMembership(userId);
        
        if (membershipOpt.isPresent()) {
            MembershipDTO dto = membershipOpt.get();
            response.put("success", true);
            response.put("data", dto);
        } else {
            response.put("success", false);
            response.put("message", "用户暂无有效会员");
        }
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * 使用支付宝购买 VIP 会员
     * @param userId 用户ID
     * @param membershipLevelId 会员等级ID
     * @param couponId 用户优惠券ID（可选）
     * @return 支付宝支付表单HTML
     */
    @PostMapping("/buy-vip")
    @ResponseBody
    public Result buyVipWithAlipay(@RequestParam Long userId, 
                                    @RequestParam Long membershipLevelId,
                                    @RequestParam(required = false) Long couponId) {
        logger.info("收到VIP购买请求，用户ID: {}, 会员等级ID: {}", userId, membershipLevelId);
        
        try {
            // 1. 参数校验
            if (userId == null || membershipLevelId == null) {
                return Result.error("用户ID和会员等级ID不能为空");
            }
            
            // 2. 验证用户是否存在
            UserInfo user = userInfoRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + userId));
            
            // 3. 验证会员等级是否存在且可用
            MembershipLevel membershipLevel = membershipLevelRepository.findById(membershipLevelId)
                    .orElseThrow(() -> new RuntimeException("会员等级不存在，ID: " + membershipLevelId));
            
            if (!Boolean.TRUE.equals(membershipLevel.getIsEnabled())) {
                return Result.error("该会员等级已停用");
            }
            
            // 4. 计算订单金额（应用优惠券折扣）
            BigDecimal originalAmount = membershipLevel.getPrice();
            BigDecimal finalAmount = originalAmount;
            
            if (couponId != null) {
                // 验证用户优惠券是否存在且可用
                UserCoupon userCoupon = userCouponRepository.findById(couponId)
                        .orElseThrow(() -> new RuntimeException("优惠券不存在，ID: " + couponId));
                
                if (!Objects.equals(userCoupon.getUserId(), userId)) {
                    return Result.error("无权使用该优惠券");
                }
                
                if (userCoupon.getReceiveStatus() != UserCoupon.ReceiveStatus.received) {
                    return Result.error("优惠券状态不正确，当前状态: " + userCoupon.getReceiveStatus() + "，无法使用");
                }
                
                // 检查优惠券是否过期
                if (userCoupon.getEndTime() != null && userCoupon.getEndTime().isBefore(LocalDateTime.now())) {
                    return Result.error("优惠券已过期，过期时间: " + userCoupon.getEndTime());
                }
                
                // 计算优惠后的金额
                Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                        .orElseThrow(() -> new RuntimeException("优惠券模板不存在"));
                
                finalAmount = calculateDiscountedAmount(coupon, originalAmount);
                
                logger.info("使用优惠券购买VIP: userCouponId={}, 原价={}, 优惠价={}, 优惠金额={}", 
                    couponId, originalAmount, finalAmount, originalAmount.subtract(finalAmount));
            }
            
            // 5. 生成订单号
            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String random = UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 8);
            String orderNo = "VIP" + time + random;
            
            // 6. 创建支付订单
            PaymentOrder order = new PaymentOrder();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setType("membership");
            order.setMembershipLevelId(membershipLevelId);
            order.setAmount(finalAmount); // 使用优惠后的金额
            order.setOriginalAmount(originalAmount); // 保存原始金额
            order.setPaymentMethod("alipay");
            order.setStatus(OrderStatus.PENDING.name());
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            
            paymentOrderRepository.save(order);
            logger.info("VIP订单创建成功，订单号: {}, 原价: {}, 优惠价: {}", orderNo, originalAmount, finalAmount);
            
            // 如果使用了优惠券，锁定优惠券
            final Long lockedCouponId = couponId;
            final Long orderId = order.getId();
            if (lockedCouponId != null) {
                try {
                    userCouponService.lockCoupon(lockedCouponId, orderId);
                    logger.info("优惠券锁定成功: userCouponId={}, orderId={}", lockedCouponId, orderId);
                } catch (Exception e) {
                    logger.error("优惠券锁定失败: {}", e.getMessage());
                    paymentOrderRepository.delete(order);
                    return Result.error("优惠券锁定失败，请重试");
                }
            }
            
            // 7. 调用支付宝接口生成支付表单
            String subject = "VIP会员 - " + membershipLevel.getName() + " (" + membershipLevel.getDurationDays() + "天)";
            
            String alipayForm = payUtil.sendRequestToAlipay(
                orderNo, 
                finalAmount.floatValue(), // 使用优惠后的金额
                subject
            );
            
            logger.info("支付宝支付表单生成成功，订单号: {}, 金额: {}", orderNo, finalAmount);
            
            // 8. 返回支付表单
            Map<String, Object> data = new HashMap<>();
            data.put("orderNo", orderNo);
            data.put("amount", finalAmount);
            data.put("originalAmount", originalAmount);
            data.put("discountAmount", originalAmount.subtract(finalAmount));
            data.put("paymentForm", alipayForm);
            
            return Result.success(data);
            
        } catch (AlipayApiException e) {
            logger.error("调用支付宝接口失败", e);
            return Result.error("调用支付宝接口失败: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("VIP购买业务异常", e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("VIP购买系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }
    
    /**
     * 计算优惠后的金额
     */
    private BigDecimal calculateDiscountedAmount(Coupon coupon, BigDecimal originalAmount) {
        BigDecimal discountAmount;
        if (coupon.getType() == Coupon.CouponType.discount) {
            // 折扣券：value表示打几折（如8.8表示打88折，支付88%，优惠12%）
            // payRate = value / 10  （支付比例）
            // discountRate = 1 - payRate  （优惠比例）
            BigDecimal payRate = coupon.getValue().divide(BigDecimal.valueOf(10), 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal discountRate = BigDecimal.ONE.subtract(payRate);
            discountAmount = originalAmount.multiply(discountRate);
        } else {
            // 现金减免券：直接使用value
            discountAmount = coupon.getValue();
        }

        // 确保优惠金额不超过订单金额
        if (discountAmount.compareTo(originalAmount) > 0) {
            discountAmount = originalAmount;
        }

        return originalAmount.subtract(discountAmount);
    }
}
