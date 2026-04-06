package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.ContentPurchaseRequest;
import com.alikeyou.itmodulepayment.dto.ContentPurchaseResponse;
import com.alikeyou.itmodulepayment.entity.PaidContent;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.RevenueRecord;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.RevenueRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
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

    public boolean hasPurchasedBlog(Long userId, Long blogId) {
        PaidContent paidContent = paidContentRepository.findByBlogId(blogId);
        if (paidContent == null) {
            return false;
        }

        String accessType = normalize(paidContent.getAccessType());

        if ("one_time".equals(accessType)) {
            List<PaymentOrder> orders = paymentOrderRepository.findByPaidContentId(paidContent.getId());
            return orders.stream().anyMatch(order ->
                    order.getUserId() != null
                            && order.getUserId().equals(userId)
                            && "paid".equalsIgnoreCase(normalize(order.getStatus()))
            );
        }

        if ("member_only".equals(accessType)) {
            UserInfo user = userInfoRepository.findById(userId).orElse(null);
            return isActiveVip(user);
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
            response.setMessage("该博客不是按次付费内容");
            return response;
        }

        if (!"published".equalsIgnoreCase(normalize(paidContent.getStatus()))) {
            response.setSuccess(false);
            response.setMessage("该付费内容当前不可购买");
            return response;
        }

        String accessType = normalize(paidContent.getAccessType());
        if (!"one_time".equals(accessType)) {
            response.setSuccess(false);
            response.setMessage("该内容不是按次付费内容");
            return response;
        }

        if (paidContent.getPrice() == null || paidContent.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
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
        order.setType("CONTENT");
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

        if (!"pending".equalsIgnoreCase(normalize(order.getStatus()))) {
            if ("paid".equalsIgnoreCase(normalize(order.getStatus()))) {
                return;
            }
            throw new RuntimeException("订单状态不正确，当前状态：" + order.getStatus());
        }

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        order.setStatus("paid");
        order.setPayTime(LocalDateTime.now());
        paymentOrderRepository.save(order);

        PaidContent paidContent = paidContentRepository.findById(order.getPaidContentId())
                .orElseThrow(() -> new RuntimeException("付费内容不存在"));

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
        revenueRecord.setSettlementStatus("UNSETTLED");
        revenueRecord.setCreatedAt(LocalDateTime.now());
        revenueRecord.setUpdatedAt(LocalDateTime.now());
        revenueRecordRepository.save(revenueRecord);

        UserInfo author = userInfoRepository.findById(authorId).orElse(null);
        if (author != null) {
            BigDecimal currentBalance = author.getBalance() != null ? author.getBalance() : BigDecimal.ZERO;
            author.setBalance(currentBalance.add(authorRevenue));
            userInfoRepository.save(author);
        }

        logger.info("购买完成，订单号: {}, 用户ID: {}, 金额: {}", order.getOrderNo(), userId, order.getAmount());
    }

    private boolean isActiveVip(UserInfo user) {
        if (user == null || !Boolean.TRUE.equals(user.getIsPremiumMember())) {
            return false;
        }
        Instant expiry = user.getPremiumExpiryDate();
        if (expiry == null) {
            return false;
        }
        return expiry.isAfter(Instant.now());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}
