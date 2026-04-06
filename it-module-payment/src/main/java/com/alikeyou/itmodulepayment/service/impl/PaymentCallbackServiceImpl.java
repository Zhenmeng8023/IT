package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.entity.*;
import com.alikeyou.itmodulepayment.repository.*;
import com.alikeyou.itmodulepayment.service.PaymentCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCallbackServiceImpl.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserInfoRepository userInfoRepository;
    private final MembershipLevelRepository membershipLevelRepository;
    private final MembershipRepository membershipRepository;
    private final PaidContentRepository paidContentRepository;
    private final RevenueRecordRepository revenueRecordRepository;
    private final UserPurchaseRepository userPurchaseRepository;

    @Value("${alipay.app-id}")
    private String alipayAppId;

    @Value("${alipay.private-key}")
    private String alipayPrivateKey;

    @Value("${alipay.public-key}")
    private String alipayPublicKey;

    @Value("${wechat.app-id}")
    private String wechatAppId;

    @Value("${wechat.mch-id}")
    private String wechatMchId;

    @Value("${wechat.api-key}")
    private String wechatApiKey;

    public PaymentCallbackServiceImpl(PaymentOrderRepository paymentOrderRepository,
                                      PaymentRecordRepository paymentRecordRepository,
                                      UserInfoRepository userInfoRepository,
                                      MembershipLevelRepository membershipLevelRepository,
                                      MembershipRepository membershipRepository,
                                      PaidContentRepository paidContentRepository,
                                      RevenueRecordRepository revenueRecordRepository,
                                      UserPurchaseRepository userPurchaseRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.userInfoRepository = userInfoRepository;
        this.membershipLevelRepository = membershipLevelRepository;
        this.membershipRepository = membershipRepository;
        this.paidContentRepository = paidContentRepository;
        this.revenueRecordRepository = revenueRecordRepository;
        this.userPurchaseRepository = userPurchaseRepository;
    }

    @Override
    public String handleAlipayCallback(String callbackData) {
        logger.info("收到支付宝回调，回调数据：{}", callbackData);
        try {
            Map<String, String> params = parseAlipayCallback(callbackData);
            if (params.isEmpty()) {
                logger.error("支付宝回调参数解析失败");
                return "fail";
            }
            boolean signVerified = verifyAlipaySign(params);
            if (signVerified) {
                String orderNo = params.get("out_trade_no");
                String tradeStatus = params.get("trade_status");
                String totalAmount = params.get("total_amount");
                String transactionId = params.get("trade_no");
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    updatePaymentStatus(orderNo, true, "alipay", transactionId, totalAmount);
                    return "success";
                }
            }
        } catch (Exception e) {
            logger.error("处理支付宝回调失败", e);
        }
        return "fail";
    }

    @Override
    public String handleWechatCallback(String callbackData) {
        logger.info("收到微信支付回调，回调数据：{}", callbackData);
        try {
            Map<String, String> params = parseWechatCallback(callbackData);
            if (params.isEmpty()) {
                logger.error("微信支付回调参数解析失败");
                return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[参数解析失败]]></return_msg></xml>";
            }
            boolean signVerified = verifyWechatSign(params);
            if (signVerified) {
                String orderNo = params.get("out_trade_no");
                String resultCode = params.get("result_code");
                String totalAmount = params.get("total_fee");
                String transactionId = params.get("transaction_id");
                if ("SUCCESS".equals(resultCode)) {
                    String amountInYuan = new BigDecimal(totalAmount)
                            .divide(new BigDecimal(100))
                            .setScale(2, java.math.RoundingMode.HALF_UP)
                            .toString();
                    updatePaymentStatus(orderNo, true, "wechat", transactionId, amountInYuan);
                    return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
                }
            }
        } catch (Exception e) {
            logger.error("处理微信支付回调失败", e);
        }
        return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[签名验证失败]]></return_msg></xml>";
    }

    private Map<String, String> parseAlipayCallback(String callbackData) {
        Map<String, String> params = new HashMap<>();
        try {
            String[] pairs = callbackData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(keyValue[0], value);
                }
            }
        } catch (Exception e) {
            logger.error("解析支付宝回调数据失败", e);
        }
        return params;
    }

    private Map<String, String> parseWechatCallback(String callbackData) {
        Map<String, String> params = new HashMap<>();
        try {
            if (callbackData.contains("out_trade_no")) {
                int start = callbackData.indexOf("<out_trade_no>") + 14;
                int end = callbackData.indexOf("</out_trade_no>");
                if (start > 14 && end > start) {
                    params.put("out_trade_no", callbackData.substring(start, end));
                }
            }
            if (callbackData.contains("result_code")) {
                int start = callbackData.indexOf("<result_code>") + 13;
                int end = callbackData.indexOf("</result_code>");
                if (start > 13 && end > start) {
                    params.put("result_code", callbackData.substring(start, end));
                }
            }
        } catch (Exception e) {
            logger.error("解析微信支付回调数据失败", e);
        }
        return params;
    }

    private boolean verifyAlipaySign(Map<String, String> params) {
        try {
            return params.containsKey("out_trade_no") && params.containsKey("trade_status");
        } catch (Exception e) {
            logger.error("支付宝签名验证失败", e);
            return false;
        }
    }

    private boolean verifyWechatSign(Map<String, String> params) {
        try {
            return params.containsKey("out_trade_no") && params.containsKey("result_code");
        } catch (Exception e) {
            logger.error("微信支付签名验证失败", e);
            return false;
        }
    }

    @Transactional
    protected void updatePaymentStatus(String orderNo, boolean isPaid, String platform, String transactionId, String amount) {
        if (!isPaid) {
            return;
        }

        PaymentOrder order = paymentOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if ("paid".equalsIgnoreCase(normalize(order.getStatus()))) {
            logger.info("订单已经是已支付状态，无需重复处理，订单号：{}", orderNo);
            return;
        }

        order.setStatus("paid");
        order.setPayTime(LocalDateTime.now());
        paymentOrderRepository.save(order);

        List<PaymentRecord> existingRecords = paymentRecordRepository.findByOrderId(order.getId());
        if (existingRecords.isEmpty()) {
            PaymentRecord record = new PaymentRecord();
            record.setOrderId(order.getId());
            record.setPaymentPlatform(platform);
            record.setTransactionId(transactionId);
            record.setPaymentStatus("success");
            record.setPaymentAmount(new BigDecimal(amount));
            record.setPaymentTime(LocalDateTime.now());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            paymentRecordRepository.save(record);
        }

        String type = normalize(order.getType());
        if ("membership".equals(type) && order.getMembershipLevelId() != null) {
            syncMembershipAfterPaid(order.getUserId(), order.getMembershipLevelId());
        } else if ("content".equals(type) && order.getPaidContentId() != null) {
            handleContentPurchaseComplete(order);
        }
    }

    private void handleContentPurchaseComplete(PaymentOrder order) {
        try {
            PaidContent paidContent = paidContentRepository.findById(order.getPaidContentId())
                    .orElseThrow(() -> new RuntimeException("付费内容不存在，ID: " + order.getPaidContentId()));

            ensureUserPurchase(order);

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
        } catch (Exception e) {
            logger.error("处理内容购买完成失败，订单号: {}", order.getOrderNo(), e);
        }
    }

    private void ensureUserPurchase(PaymentOrder order) {
        UserPurchase userPurchase = userPurchaseRepository.findByUserIdAndPaidContentId(order.getUserId(), order.getPaidContentId())
                .orElseGet(UserPurchase::new);
        userPurchase.setUserId(order.getUserId());
        userPurchase.setPaidContentId(order.getPaidContentId());
        userPurchase.setOrderId(order.getId());
        userPurchase.setPurchaseTime(order.getPayTime() != null ? order.getPayTime() : LocalDateTime.now());
        userPurchase.setAccessExpiredAt(null);
        userPurchaseRepository.save(userPurchase);
    }

    private void syncMembershipAfterPaid(Long userId, Long membershipLevelId) {
        try {
            MembershipLevel level = membershipLevelRepository.findById(membershipLevelId)
                    .orElseThrow(() -> new RuntimeException("会员等级不存在，ID: " + membershipLevelId));
            UserInfo user = userInfoRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + userId));

            LocalDateTime now = LocalDateTime.now();
            List<Membership> expiredMemberships = membershipRepository.findByUserIdAndStatusAndEndTimeBefore(userId, "active", now);
            for (Membership expired : expiredMemberships) {
                expired.setStatus("expired");
            }
            if (!expiredMemberships.isEmpty()) {
                membershipRepository.saveAll(expiredMemberships);
            }

            Optional<Membership> activeOpt = membershipRepository.findTopByUserIdAndStatusAndEndTimeAfterOrderByEndTimeDesc(userId, "active", now);
            LocalDateTime startTime;
            LocalDateTime endTime;
            Membership target;

            if (activeOpt.isPresent()) {
                target = activeOpt.get();
                startTime = target.getStartTime() != null ? target.getStartTime() : now;
                LocalDateTime currentEnd = target.getEndTime() != null && target.getEndTime().isAfter(now) ? target.getEndTime() : now;
                endTime = currentEnd.plusDays(level.getDurationDays());
                target.setLevelId(level.getId());
            } else {
                target = new Membership();
                target.setUserId(userId);
                target.setLevelId(level.getId());
                startTime = now;
                endTime = now.plusDays(level.getDurationDays());
            }

            target.setStartTime(startTime);
            target.setEndTime(endTime);
            target.setStatus("active");
            target.setCreatedAt(target.getCreatedAt() == null ? now : target.getCreatedAt());
            target.setUpdatedAt(now);
            membershipRepository.save(target);

            user.setIsPremiumMember(true);
            user.setPremiumExpiryDate(endTime.atZone(ZoneId.systemDefault()).toInstant());
            userInfoRepository.save(user);

            logger.info("会员订阅同步成功，用户ID: {}, 等级: {}, 到期时间: {}", userId, level.getName(), endTime);
        } catch (Exception e) {
            logger.error("同步会员表失败，用户ID: {}, 会员等级ID: {}", userId, membershipLevelId, e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
