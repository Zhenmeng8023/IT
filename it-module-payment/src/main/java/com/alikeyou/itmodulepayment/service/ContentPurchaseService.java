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
import com.alikeyou.itmodulepayment.entity.UserPurchase;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.MembershipRepository;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.RevenueRecordRepository;
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

        PaymentOrder order = new PaymentOrder();
        String orderNo = "CP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setType("content");
        order.setTargetId(paidContent.getId());
        order.setPaidContentId(paidContent.getId());
        order.setAmount(paidContent.getPrice());
        order.setPaymentMethod(paymentMethod);
        order.setStatus("pending");

        paymentOrderRepository.save(order);

        response.setSuccess(true);
        response.setAlreadyPurchased(false);
        response.setOrderNo(orderNo);
        response.setAmount(paidContent.getPrice());
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
}
