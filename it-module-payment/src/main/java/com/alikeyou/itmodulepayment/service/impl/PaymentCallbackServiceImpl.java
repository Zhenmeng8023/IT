package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.entity.OrderStatus;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.PaymentRecordRepository;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.service.PaymentCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCallbackServiceImpl.class);
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserInfoRepository userInfoRepository;
    private final MembershipLevelRepository membershipLevelRepository;

    // 支付宝配置
    @Value("${alipay.app-id}")
    private String alipayAppId;

    @Value("${alipay.private-key}")
    private String alipayPrivateKey;

    @Value("${alipay.public-key}")
    private String alipayPublicKey;

    // 微信支付配置
    @Value("${wechat.app-id}")
    private String wechatAppId;

    @Value("${wechat.mch-id}")
    private String wechatMchId;

    @Value("${wechat.api-key}")
    private String wechatApiKey;

    public PaymentCallbackServiceImpl(PaymentOrderRepository paymentOrderRepository, 
                                       PaymentRecordRepository paymentRecordRepository,
                                       UserInfoRepository userInfoRepository,
                                       MembershipLevelRepository membershipLevelRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.userInfoRepository = userInfoRepository;
        this.membershipLevelRepository = membershipLevelRepository;
    }

    @Override
    public String handleAlipayCallback(String callbackData) {
        logger.info("收到支付宝回调，回调数据：{}", callbackData);
        try {
            // 解析支付宝回调数据
            Map<String, String> params = parseAlipayCallback(callbackData);
            
            if (params.isEmpty()) {
                logger.error("支付宝回调参数解析失败");
                return "fail";
            }

            // 验证签名
            boolean signVerified = verifyAlipaySign(params);

            if (signVerified) {
                String orderNo = params.get("out_trade_no");
                String tradeStatus = params.get("trade_status");
                String totalAmount = params.get("total_amount");
                String transactionId = params.get("trade_no");
                
                logger.info("支付宝回调验证通过，订单号：{}, 交易状态：{}, 金额：{}, 支付宝交易号：{}", 
                    orderNo, tradeStatus, totalAmount, transactionId);

                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    updatePaymentStatus(orderNo, true, "alipay", transactionId, totalAmount);
                    logger.info("支付宝支付成功，订单号：{}", orderNo);
                    return "success";
                } else {
                    logger.info("支付宝交易状态不是成功，订单号：{}, 交易状态：{}", orderNo, tradeStatus);
                }
            } else {
                logger.error("支付宝回调签名验证失败，回调数据：{}", callbackData);
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
            // 解析微信支付回调数据
            Map<String, String> params = parseWechatCallback(callbackData);
            
            if (params.isEmpty()) {
                logger.error("微信支付回调参数解析失败");
                return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[参数解析失败]]></return_msg></xml>";
            }

            // 验证签名
            boolean signVerified = verifyWechatSign(params);

            if (signVerified) {
                String orderNo = params.get("out_trade_no");
                String resultCode = params.get("result_code");
                String totalAmount = params.get("total_fee");
                String transactionId = params.get("transaction_id");
                
                logger.info("微信支付回调验证通过，订单号：{}, 结果码：{}, 金额：{} 分，微信交易号：{}", 
                    orderNo, resultCode, totalAmount, transactionId);

                if ("SUCCESS".equals(resultCode)) {
                    // 将分转换为元
                    String amountInYuan = new java.math.BigDecimal(totalAmount)
                        .divide(new java.math.BigDecimal(100))
                        .setScale(2, java.math.RoundingMode.HALF_UP)
                        .toString();
                    updatePaymentStatus(orderNo, true, "wechat", transactionId, amountInYuan);
                    logger.info("微信支付成功，订单号：{}", orderNo);
                    return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
                } else {
                    logger.info("微信支付结果不是成功，订单号：{}, 结果码：{}", orderNo, resultCode);
                }
            } else {
                logger.error("微信支付回调签名验证失败，回调数据：{}", callbackData);
            }
        } catch (Exception e) {
            logger.error("处理微信支付回调失败", e);
        }
        return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[签名验证失败]]></return_msg></xml>";
    }

    private Map<String, String> parseAlipayCallback(String callbackData) {
        // 解析支付宝回调数据
        // 实际实现需要解析HTTP请求参数
        Map<String, String> params = new java.util.HashMap<>();
        try {
            // 模拟解析逻辑
            String[] pairs = callbackData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    // 解码URL编码的参数值
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
        // 解析微信支付回调数据
        // 实际实现需要解析XML数据
        Map<String, String> params = new java.util.HashMap<>();
        try {
            // 模拟解析逻辑
            // 实际项目中应使用XML解析库，如DOM4J或JAXB
            // 这里简单模拟，实际实现需要解析XML字符串
            if (callbackData.contains("out_trade_no")) {
                // 提取订单号
                int start = callbackData.indexOf("<out_trade_no>") + 14;
                int end = callbackData.indexOf("</out_trade_no>");
                if (start > 14 && end > start) {
                    String orderNo = callbackData.substring(start, end);
                    params.put("out_trade_no", orderNo);
                }
            }
            if (callbackData.contains("result_code")) {
                // 提取结果码
                int start = callbackData.indexOf("<result_code>") + 13;
                int end = callbackData.indexOf("</result_code>");
                if (start > 13 && end > start) {
                    String resultCode = callbackData.substring(start, end);
                    params.put("result_code", resultCode);
                }
            }
        } catch (Exception e) {
            logger.error("解析微信支付回调数据失败", e);
        }
        return params;
    }

    private boolean verifyAlipaySign(Map<String, String> params) {
        // 验证支付宝签名
        // 实际实现需要使用支付宝SDK
        try {
            // 检查是否包含必要的参数
            if (!params.containsKey("out_trade_no") || !params.containsKey("trade_status")) {
                logger.error("支付宝回调参数不完整");
                return false;
            }
            
            // 实际项目中应使用支付宝SDK的验签方法
            // 例如：AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2")
            // 这里模拟签名验证通过
            return true;
        } catch (Exception e) {
            logger.error("支付宝签名验证失败", e);
            return false;
        }
    }

    private boolean verifyWechatSign(Map<String, String> params) {
        // 验证微信支付签名
        // 实际实现需要使用微信支付SDK
        try {
            // 检查是否包含必要的参数
            if (!params.containsKey("out_trade_no") || !params.containsKey("result_code")) {
                logger.error("微信支付回调参数不完整");
                return false;
            }
            
            // 实际项目中应使用微信支付SDK的验签方法
            // 例如：WXPayUtil.isSignatureValid(params, wechatApiKey)
            // 这里模拟签名验证通过
            return true;
        } catch (Exception e) {
            logger.error("微信支付签名验证失败", e);
            return false;
        }
    }

    @Transactional
    protected void updatePaymentStatus(String orderNo, boolean isPaid, String platform, String transactionId, String amount) {
        if (isPaid) {
            // 更新订单状态
            PaymentOrder order = paymentOrderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));
            
            // 检查订单状态，避免重复处理
            if (OrderStatus.PAID.name().equals(order.getStatus())) {
                logger.info("订单已经是已支付状态，无需重复处理，订单号：{}", orderNo);
                return;
            }
            
            order.setStatus(OrderStatus.PAID.name());
            order.setPayTime(LocalDateTime.now());
            paymentOrderRepository.save(order);
            logger.info("订单状态更新为已支付，订单号：{}", orderNo);

            // 检查是否已存在支付记录（通过交易 ID 判断）
            List<PaymentRecord> existingRecords = paymentRecordRepository.findByOrderId(order.getId());
            if (!existingRecords.isEmpty()) {
                logger.warn("支付记录已存在，订单号：{}", orderNo);
                return;
            }
            
            // 创建支付记录
            PaymentRecord record = new PaymentRecord();
            record.setOrderId(order.getId());
            record.setPaymentPlatform(platform);
            record.setTransactionId(transactionId); // 设置第三方交易 ID
            record.setPaymentStatus("SUCCESS");
            record.setPaymentAmount(new java.math.BigDecimal(amount)); // 设置实际支付金额
            record.setPaymentTime(LocalDateTime.now());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            paymentRecordRepository.save(record);
            logger.info("支付记录创建成功，订单号：{}, 支付平台：{}, 交易 ID: {}", orderNo, platform, transactionId);
            
            // 如果是会员订单，更新用户的 VIP 状态
            if ("membership".equals(order.getType()) && order.getMembershipLevelId() != null) {
                updateUserVipStatus(order.getUserId(), order.getMembershipLevelId());
            }
        }
    }
    
    /**
     * 更新用户的 VIP 状态
     * @param userId 用户 ID
     * @param membershipLevelId 会员等级 ID
     */
    private void updateUserVipStatus(Long userId, Long membershipLevelId) {
        try {
            // 查询会员等级信息
            MembershipLevel membershipLevel = membershipLevelRepository.findById(membershipLevelId)
                    .orElseThrow(() -> new RuntimeException("会员等级不存在，ID: " + membershipLevelId));
            
            // 查询用户信息
            UserInfo user = userInfoRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + userId));
            
            // 设置 VIP 状态为 true
            user.setIsPremiumMember(true);
            
            // 计算新的过期时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime newExpiryTime;
            
            // 如果用户已有 VIP 状态且未过期，则在原有过期时间上累加
            if (user.getPremiumExpiryDate() != null) {
                Instant expiryInstant = user.getPremiumExpiryDate();
                LocalDateTime currentExpiryTime = LocalDateTime.ofInstant(expiryInstant, ZoneId.systemDefault());
                
                // 如果当前 VIP 已过期，则从现在开始计算
                if (currentExpiryTime.isBefore(now)) {
                    newExpiryTime = now.plusDays(membershipLevel.getDurationDays());
                } else {
                    // 如果当前 VIP 未过期，则累加天数
                    newExpiryTime = currentExpiryTime.plusDays(membershipLevel.getDurationDays());
                }
            } else {
                // 如果没有过期时间，从现在开始计算
                newExpiryTime = now.plusDays(membershipLevel.getDurationDays());
            }
            
            // 设置过期时间
            user.setPremiumExpiryDate(newExpiryTime.atZone(ZoneId.systemDefault()).toInstant());
            
            // 保存用户信息
            userInfoRepository.save(user);
            
            logger.info("用户 VIP 状态更新成功，用户 ID: {}, 会员等级：{}, 到期时间：{}", 
                       userId, membershipLevel.getName(), newExpiryTime);
            
        } catch (Exception e) {
            logger.error("更新用户 VIP 状态失败，用户 ID: {}, 会员等级 ID: {}", userId, membershipLevelId, e);
            // 这里不抛出异常，避免影响主流程
        }
    }
}