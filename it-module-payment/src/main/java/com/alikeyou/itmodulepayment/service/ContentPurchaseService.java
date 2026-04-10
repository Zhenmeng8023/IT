package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.ContentPurchaseRequest;
import com.alikeyou.itmodulepayment.dto.ContentPurchaseResponse;
import com.alikeyou.itmodulepayment.entity.Membership;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.entity.PaidContent;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.RevenueRecord;
import com.alikeyou.itmodulepayment.entity.UserCoupon;
import com.alikeyou.itmodulepayment.entity.UserPurchase;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.MembershipRepository;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.RevenueRecordRepository;
import com.alikeyou.itmodulepayment.repository.UserCouponRepository;
import com.alikeyou.itmodulepayment.repository.UserPurchaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContentPurchaseService {

    private static final Logger logger = LoggerFactory.getLogger(ContentPurchaseService.class);

    @Autowired
    private PaidContentRepository paidContentRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private RevenueRecordRepository revenueRecordRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private MembershipLevelRepository membershipLevelRepository;

    @Autowired
    private UserPurchaseRepository userPurchaseRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private com.alikeyou.itmodulepayment.service.UserCouponService userCouponService;

    @Autowired
    private com.alikeyou.itmodulepayment.service.CouponRedemptionService redemptionService;

    @Autowired
    private com.alikeyou.itmodulepayment.repository.CouponRepository couponRepository;

    public boolean hasPurchasedBlog(Long userId, Long blogId) {
        PaidContent paidContent = paidContentRepository.findByBlogId(blogId);
        if (paidContent == null) {
            return false;
        }

        String accessType = normalize(paidContent.getAccessType());
        if ("one_time".equals(accessType)) {
            Optional<UserPurchase> purchaseOpt = userPurchaseRepository.findByUserIdAndPaidContentId(userId, paidContent.getId());
            if (purchaseOpt.isPresent()) {
                UserPurchase purchase = purchaseOpt.get();
                return purchase.getAccessExpiredAt() == null || purchase.getAccessExpiredAt().isAfter(LocalDateTime.now());
            }
            return false;
        }

        if ("member_only".equals(accessType)) {
            return hasActiveMembershipAccess(userId, paidContent.getRequiredMembershipLevelId());
        }

        return false;
    }

    @Transactional
    public ContentPurchaseResponse createPurchaseOrder(ContentPurchaseRequest request, Long userId) {
        ContentPurchaseResponse response = new ContentPurchaseResponse();

        if (request == null || request.getBlogId() == null) {
            response.setSuccess(false);
            response.setMessage("博客 ID 不能为空");
            return response;
        }

        PaidContent paidContent = paidContentRepository.findByBlogId(request.getBlogId());
        if (paidContent == null) {
            response.setSuccess(false);
            response.setMessage("该博客暂未生成付费记录，请先重新保存博客后再试");
            return response;
        }

        if (!"published".equalsIgnoreCase(normalize(paidContent.getStatus()))) {
            response.setSuccess(false);
            response.setMessage("该付费内容当前不可购买");
            return response;
        }

        String accessType = normalize(paidContent.getAccessType());
        if ("member_only".equals(accessType)) {
            response.setSuccess(false);
            response.setMessage("该内容为会员专属，请先购买会员后访问");
            return response;
        }

        if (!"one_time".equals(accessType) || paidContent.getPrice() == null || paidContent.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            response.setSuccess(false);
            response.setMessage("该博客不是按次付费内容");
            return response;
        }

        if (hasPurchasedBlog(userId, request.getBlogId())) {
            response.setSuccess(true);
            response.setAlreadyPurchased(true);
            response.setMessage("您已购买过该博客");
            return response;
        }

        String paymentMethod = normalize(request.getPaymentMethod());
        if (!"wechat".equals(paymentMethod) && !"alipay".equals(paymentMethod)) {
            response.setSuccess(false);
            response.setMessage("不支持的支付方式，请使用微信支付或支付宝支付");
            return response;
        }

        // 处理优惠券逻辑
        BigDecimal originalAmount = paidContent.getPrice();
        BigDecimal finalAmount = originalAmount;
        Long userCouponId = request.getUserCouponId();
        
        if (userCouponId != null) {
            try {
                // 验证用户优惠券是否存在且可用
                UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                        .orElseThrow(() -> new RuntimeException("优惠券不存在"));
                
                if (!Objects.equals(userCoupon.getUserId(), userId)) {
                    throw new RuntimeException("无权使用该优惠券");
                }
                
                if (userCoupon.getReceiveStatus() != UserCoupon.ReceiveStatus.received) {
                    throw new RuntimeException("优惠券状态不正确，无法使用");
                }
                
                // 检查优惠券是否过期
                if (userCoupon.getEndTime() != null && userCoupon.getEndTime().isBefore(LocalDateTime.now())) {
                    throw new RuntimeException("优惠券已过期");
                }
                
                // 计算优惠后的金额
                com.alikeyou.itmodulepayment.entity.Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                        .orElseThrow(() -> new RuntimeException("优惠券模板不存在"));
                
                finalAmount = calculateDiscountedAmount(coupon, originalAmount);
                
                logger.info("使用优惠券: userCouponId={}, 原价={}, 优惠价={}", userCouponId, originalAmount, finalAmount);
            } catch (Exception e) {
                logger.warn("优惠券处理失败，将按原价创建订单: {}", e.getMessage());
                userCouponId = null; // 如果优惠券处理失败，则不使用优惠券
                finalAmount = originalAmount;
            }
        }

        PaymentOrder order = new PaymentOrder();
        String orderNo = "CP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setType("content");
        order.setTargetId(paidContent.getId());
        order.setPaidContentId(paidContent.getId());
        order.setAmount(finalAmount); // 使用优惠后的金额
        order.setPaymentMethod(paymentMethod);
        order.setStatus("pending");

        paymentOrderRepository.save(order);
        
        // 如果使用了优惠券，锁定优惠券
        if (userCouponId != null) {
            try {
                userCouponService.lockCoupon(userCouponId, order.getId());
                logger.info("优惠券锁定成功: userCouponId={}, orderId={}", userCouponId, order.getId());
            } catch (Exception e) {
                logger.error("优惠券锁定失败: {}", e.getMessage());
                // 如果锁定失败，回滚订单
                paymentOrderRepository.delete(order);
                response.setSuccess(false);
                response.setMessage("优惠券锁定失败，请重试");
                return response;
            }
        }

        response.setSuccess(true);
        response.setAlreadyPurchased(false);
        response.setOrderNo(orderNo);
        response.setAmount(finalAmount);
        response.setOriginalAmount(originalAmount);
        response.setDiscountAmount(originalAmount.subtract(finalAmount));
        response.setUserCouponId(userCouponId);
        response.setMessage("订单创建成功");
        return response;
    }

    @Transactional
    public void completePurchase(Long userId, Long orderId, String orderNo) {
        PaymentOrder order;
        if (orderId != null) {
            order = paymentOrderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));
        } else if (orderNo != null && !orderNo.trim().isEmpty()) {
            order = paymentOrderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));
        } else {
            throw new RuntimeException("订单ID和订单号不能同时为空");
        }

        if (!Objects.equals(order.getUserId(), userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        String currentStatus = normalize(order.getStatus());
        if (!"pending".equals(currentStatus)) {
            if ("paid".equals(currentStatus)) {
                ensureUserPurchase(order);
                return;
            }
            throw new RuntimeException("订单状态不正确，当前状态：" + order.getStatus());
        }

        order.setStatus("paid");
        order.setPayTime(LocalDateTime.now());
        paymentOrderRepository.save(order);

        PaidContent paidContent = paidContentRepository.findById(order.getPaidContentId())
                .orElseThrow(() -> new RuntimeException("付费内容不存在"));

        ensureUserPurchase(order);
        createRevenueForContentOrder(order, paidContent);
        
        // 处理优惠券核销
        processCouponRedemption(order);

        logger.info("购买完成，订单号: {}, 用户ID: {}, 金额: {}", order.getOrderNo(), userId, order.getAmount());
    }

    private void ensureUserPurchase(PaymentOrder order) {
        if (order == null || order.getPaidContentId() == null) {
            return;
        }
        UserPurchase userPurchase = userPurchaseRepository.findByUserIdAndPaidContentId(order.getUserId(), order.getPaidContentId())
                .orElseGet(UserPurchase::new);
        userPurchase.setUserId(order.getUserId());
        userPurchase.setPaidContentId(order.getPaidContentId());
        userPurchase.setOrderId(order.getId());
        userPurchase.setPurchaseTime(order.getPayTime() != null ? order.getPayTime() : LocalDateTime.now());
        userPurchase.setAccessExpiredAt(null);
        userPurchaseRepository.save(userPurchase);
    }

    private void createRevenueForContentOrder(PaymentOrder order, PaidContent paidContent) {
        Long authorId = paidContent.getCreatedBy();
        if (authorId == null) {
            logger.warn("付费内容缺少作者ID，无法分配收益，paidContentId: {}", paidContent.getId());
            return;
        }

        BigDecimal platformFee = order.getAmount().multiply(new BigDecimal("0.2"));
        BigDecimal authorRevenue = order.getAmount().subtract(platformFee);

        RevenueRecord revenueRecord = new RevenueRecord();
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

    private boolean hasActiveMembershipAccess(Long userId, Long requiredLevelId) {
        LocalDateTime now = LocalDateTime.now();
        Optional<Membership> activeMembershipOpt = membershipRepository
                .findTopByUserIdAndStatusAndEndTimeAfterOrderByEndTimeDesc(userId, "active", now);
        if (activeMembershipOpt.isPresent()) {
            Membership membership = activeMembershipOpt.get();
            if (requiredLevelId == null) {
                return true;
            }
            if (Objects.equals(membership.getLevelId(), requiredLevelId)) {
                return true;
            }
            MembershipLevel currentLevel = membershipLevelRepository.findById(membership.getLevelId()).orElse(null);
            MembershipLevel requiredLevel = membershipLevelRepository.findById(requiredLevelId).orElse(null);
            if (currentLevel != null && requiredLevel != null && currentLevel.getPriority() != null && requiredLevel.getPriority() != null) {
                return currentLevel.getPriority() >= requiredLevel.getPriority();
            }
        }

        UserInfo user = userInfoRepository.findById(userId).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsPremiumMember()) || user.getPremiumExpiryDate() == null) {
            return false;
        }
        return user.getPremiumExpiryDate().isAfter(Instant.now());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /**
     * 计算优惠后的金额
     */
    private BigDecimal calculateDiscountedAmount(com.alikeyou.itmodulepayment.entity.Coupon coupon, BigDecimal originalAmount) {
        BigDecimal discountAmount;
        if (coupon.getType() == com.alikeyou.itmodulepayment.entity.Coupon.CouponType.discount) {
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

    /**
     * 处理优惠券核销
     */
    private void processCouponRedemption(PaymentOrder order) {
        try {
            // 查询该订单是否使用了优惠券
            UserCoupon userCoupon = userCouponRepository.findByOrderId(order.getId())
                    .orElse(null);
            
            if (userCoupon != null && userCoupon.getReceiveStatus() == UserCoupon.ReceiveStatus.locked) {
                // 获取优惠券模板信息，用于计算原始金额
                com.alikeyou.itmodulepayment.entity.Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                        .orElseThrow(() -> new RuntimeException("优惠券模板不存在"));
                
                // 计算原始金额：finalAmount + discountAmount
                BigDecimal finalAmount = order.getAmount();
                BigDecimal originalAmount = calculateOriginalAmount(coupon, finalAmount);
                
                // 创建核销记录
                redemptionService.createRedemption(userCoupon.getId(), order.getId(), originalAmount);
                
                // 标记优惠券为已使用
                userCouponService.useCoupon(userCoupon.getId(), order.getId());
                
                logger.info("✅ 优惠券核销成功: userCouponId={}, orderId={}, originalAmount={}, finalAmount={}", 
                    userCoupon.getId(), order.getId(), originalAmount, finalAmount);
            }
        } catch (Exception e) {
            logger.error("❌ 优惠券核销失败: orderId={}, error={}", order.getId(), e.getMessage());
            // 核销失败不影响订单完成，只记录日志
        }
    }
    
    /**
     * 根据优惠券类型和优惠后金额，反推原始金额
     * 注意：因为 PaymentOrder.originalAmount 是 @Transient 字段，不会存入数据库，
     * 所以支付成功回调时需要通过优惠后金额反推原始金额用于核销记录
     */
    private BigDecimal calculateOriginalAmount(com.alikeyou.itmodulepayment.entity.Coupon coupon, BigDecimal finalAmount) {
        if (coupon.getType() == com.alikeyou.itmodulepayment.entity.Coupon.CouponType.discount) {
            // 折扣券正向计算：
            //   value表示打几折（如8.8表示打88折，支付88%，优惠12%）
            //   payRate = value / 10  （支付比例，如0.88）
            //   finalAmount = originalAmount × payRate
            // 所以反向计算：
            //   originalAmount = finalAmount / payRate
            BigDecimal payRate = coupon.getValue().divide(BigDecimal.valueOf(10), 4, BigDecimal.ROUND_HALF_UP);
            if (payRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("无效的折扣率: " + coupon.getValue());
            }
            return finalAmount.divide(payRate, 2, BigDecimal.ROUND_HALF_UP);
        } else {
            // 现金减免券正向计算：
            //   finalAmount = originalAmount - value
            // 所以反向计算：
            //   originalAmount = finalAmount + value
            return finalAmount.add(coupon.getValue());
        }
    }
}
