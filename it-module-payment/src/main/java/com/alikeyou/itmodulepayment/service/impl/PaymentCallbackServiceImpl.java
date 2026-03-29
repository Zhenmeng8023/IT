package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.entity.OrderStatus;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.PaymentRecordRepository;
import com.alikeyou.itmodulepayment.service.PaymentCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCallbackServiceImpl.class);
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;

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

    public PaymentCallbackServiceImpl(PaymentOrderRepository paymentOrderRepository, PaymentRecordRepository paymentRecordRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Override
    public String handleAlipayCallback(String callbackData) {
        logger.info("收到支付宝回调");
        try {
            // 解析支付宝回调数据
            // 实际实现需要使用支付宝SDK验证签名
            Map<String, String> params = parseAlipayCallback(callbackData);

            // 验证签名
            boolean signVerified = verifyAlipaySign(params);

            if (signVerified) {
                String orderNo = params.get("out_trade_no");
                String tradeStatus = params.get("trade_status");
                logger.info("支付宝回调验证通过，订单号: {}, 交易状态: {}", orderNo, tradeStatus);

                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    updatePaymentStatus(orderNo, true, "alipay");
                    logger.info("支付宝支付成功，订单号: {}", orderNo);
                    return "success";
                } else {
                    logger.info("支付宝交易状态不是成功，订单号: {}, 交易状态: {}", orderNo, tradeStatus);
                }
            } else {
                logger.error("支付宝回调签名验证失败");
            }
        } catch (Exception e) {
            logger.error("处理支付宝回调失败", e);
        }
        return "fail";
    }

    @Override
    public String handleWechatCallback(String callbackData) {
        logger.info("收到微信支付回调");
        try {
            // 解析微信支付回调数据
            // 实际实现需要使用微信支付SDK验证签名
            Map<String, String> params = parseWechatCallback(callbackData);

            // 验证签名
            boolean signVerified = verifyWechatSign(params);

            if (signVerified) {
                String orderNo = params.get("out_trade_no");
                String resultCode = params.get("result_code");
                logger.info("微信支付回调验证通过，订单号: {}, 结果码: {}", orderNo, resultCode);

                if ("SUCCESS".equals(resultCode)) {
                    updatePaymentStatus(orderNo, true, "wechat");
                    logger.info("微信支付成功，订单号: {}", orderNo);
                    return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
                } else {
                    logger.info("微信支付结果不是成功，订单号: {}, 结果码: {}", orderNo, resultCode);
                }
            } else {
                logger.error("微信支付回调签名验证失败");
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
    protected void updatePaymentStatus(String orderNo, boolean isPaid, String platform) {
        if (isPaid) {
            // 更新订单状态
            PaymentOrder order = paymentOrderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));
            
            // 检查订单状态，避免重复处理
            if (OrderStatus.PAID.name().equals(order.getStatus())) {
                logger.info("订单已经是已支付状态，无需重复处理，订单号: {}", orderNo);
                return;
            }
            
            order.setStatus(OrderStatus.PAID.name());
            order.setPayTime(LocalDateTime.now());
            paymentOrderRepository.save(order);
            logger.info("订单状态更新为已支付，订单号: {}", orderNo);

            // 检查是否已存在支付记录
            List<PaymentRecord> existingRecords = paymentRecordRepository.findByOrderId(order.getId());
            if (!existingRecords.isEmpty()) {
                logger.info("支付记录已存在，订单号: {}", orderNo);
                return;
            }
            
            // 创建支付记录
            PaymentRecord record = new PaymentRecord();
            record.setOrderId(order.getId());
            record.setPaymentPlatform(platform);
            record.setPaymentStatus("SUCCESS");
            record.setPaymentAmount(order.getAmount()); // 设置支付金额
            record.setPaymentTime(LocalDateTime.now());
            record.setCreatedAt(LocalDateTime.now()); // 设置创建时间
            record.setUpdatedAt(LocalDateTime.now()); // 设置更新时间
            paymentRecordRepository.save(record);
            logger.info("支付记录创建成功，订单号: {}, 支付平台: {}", orderNo, platform);
        }
    }
}