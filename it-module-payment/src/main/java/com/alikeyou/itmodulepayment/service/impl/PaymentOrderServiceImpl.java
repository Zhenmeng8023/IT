package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.PaymentOrderDTO;
import com.alikeyou.itmodulepayment.entity.OrderStatus;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.service.PaymentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentOrderServiceImpl implements PaymentOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentOrderServiceImpl.class);
    private final PaymentOrderRepository paymentOrderRepository;

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

    // 回调地址配置
    @Value("${payment.callback-domain}")
    private String callbackDomain;

    public PaymentOrderServiceImpl(PaymentOrderRepository paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Override
    @Transactional
    public PaymentOrder createOrder(PaymentOrderDTO dto) {
        logger.info("创建订单，用户ID: {}, 订单类型: {}, 金额: {}", dto.getUserId(), dto.getType(), dto.getAmount());
        PaymentOrder paymentOrder = new PaymentOrder();
        BeanUtils.copyProperties(dto, paymentOrder);
        // 生成订单号：ORDER + 时间戳 + 6位随机数
        String orderNo = "ORDER" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%06d", (int)(Math.random() * 1000000));
        paymentOrder.setOrderNo(orderNo);
        paymentOrder.setStatus(OrderStatus.PENDING.name()); // 初始状态为待支付
        paymentOrder.setCreatedAt(LocalDateTime.now());
        paymentOrder.setUpdatedAt(LocalDateTime.now());
        
        PaymentOrder savedOrder = paymentOrderRepository.save(paymentOrder);
        logger.info("订单创建成功，订单号: {}", savedOrder.getOrderNo());
        return savedOrder;
    }

    @Override
    @Transactional
    public PaymentOrder updateOrder(Long id, PaymentOrderDTO dto) {
        logger.info("更新订单，订单ID: {}", id);
        PaymentOrder paymentOrder = paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        BeanUtils.copyProperties(dto, paymentOrder);
        paymentOrder.setUpdatedAt(LocalDateTime.now());
        PaymentOrder updatedOrder = paymentOrderRepository.save(paymentOrder);
        logger.info("订单更新成功，订单号: {}", updatedOrder.getOrderNo());
        return updatedOrder;
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        logger.info("删除订单，订单ID: {}", id);
        paymentOrderRepository.deleteById(id);
        logger.info("订单删除成功，订单ID: {}", id);
    }

    @Override
    public PaymentOrder getOrderById(Long id) {
        logger.info("查询订单，订单ID: {}", id);
        return paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    @Override
    public PaymentOrder getOrderByOrderNo(String orderNo) {
        logger.info("根据订单号查询订单，订单号: {}", orderNo);
        return paymentOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    @Override
    public List<PaymentOrder> getOrdersByUserId(Long userId) {
        logger.info("根据用户ID查询订单，用户ID: {}", userId);
        return paymentOrderRepository.findByUserId(userId);
    }

    @Override
    public List<PaymentOrder> getOrdersByUserIdAndType(Long userId, String type) {
        logger.info("根据用户ID和订单类型查询订单，用户ID: {}, 订单类型: {}", userId, type);
        return paymentOrderRepository.findByUserIdAndType(userId, type);
    }

    @Override
    public List<PaymentOrder> getOrdersByStatus(String status) {
        logger.info("根据状态查询订单，状态: {}", status);
        return paymentOrderRepository.findByStatus(status);
    }

    @Override
    public List<PaymentOrder> getOrdersByPaidContentId(Long paidContentId) {
        logger.info("根据付费内容ID查询订单，付费内容ID: {}", paidContentId);
        return paymentOrderRepository.findByPaidContentId(paidContentId);
    }

    @Override
    public List<PaymentOrder> getOrdersByMembershipLevelId(Long membershipLevelId) {
        logger.info("根据会员等级ID查询订单，会员等级ID: {}", membershipLevelId);
        return paymentOrderRepository.findByMembershipLevelId(membershipLevelId);
    }

    @Override
    public String generatePaymentUrl(PaymentOrder paymentOrder, String paymentMethod) {
        logger.info("生成支付链接，订单号: {}, 支付方式: {}", paymentOrder.getOrderNo(), paymentMethod);
        if ("alipay".equals(paymentMethod)) {
            // 生成支付宝支付链接
            String url = generateAlipayUrl(paymentOrder);
            logger.info("支付宝支付链接生成成功，订单号: {}", paymentOrder.getOrderNo());
            return url;
        } else if ("wechat".equals(paymentMethod)) {
            // 生成微信支付链接
            String url = generateWechatUrl(paymentOrder);
            logger.info("微信支付链接生成成功，订单号: {}", paymentOrder.getOrderNo());
            return url;
        } else if ("balance".equals(paymentMethod)) {
            // 余额支付逻辑
            // 实际实现需要调用余额支付服务
            logger.info("余额支付处理，订单号: {}", paymentOrder.getOrderNo());
            // 这里返回一个成功的链接，实际项目中应该处理余额支付逻辑
            return callbackDomain + "/payment/success?orderNo=" + paymentOrder.getOrderNo();
        } else {
            logger.error("不支持的支付方式: {}", paymentMethod);
            throw new RuntimeException("不支持的支付方式");
        }
    }

    private String generateAlipayUrl(PaymentOrder paymentOrder) {
        // 构建支付宝支付链接
        // 实际实现需要使用支付宝SDK
        // 这里使用配置参数生成支付链接
        String subject = "支付订单";
        if (paymentOrder.getType().equals("PAID_CONTENT")) {
            subject = "购买付费内容";
        } else if (paymentOrder.getType().equals("MEMBERSHIP")) {
            subject = "购买会员";
        }
        
        String notifyUrl = callbackDomain + "/api/payment/callback/alipay";
        String returnUrl = callbackDomain + "/payment/success";
        
        // 实际项目中应使用支付宝SDK生成签名和支付链接
        // 这里模拟生成支付链接，实际项目中应集成支付宝SDK
        return "https://openapi.alipaydev.com/gateway.do?app_id=" + alipayAppId + "&method=alipay.trade.page.pay&format=JSON&charset=UTF-8&sign_type=RSA2&timestamp=" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "&version=1.0&notify_url=" + notifyUrl + "&return_url=" + returnUrl + "&biz_content={\"out_trade_no\":\"" + paymentOrder.getOrderNo() + "\",\"total_amount\":\"" + paymentOrder.getAmount() + "\",\"subject\":\"" + subject + "\"}";
    }

    private String generateWechatUrl(PaymentOrder paymentOrder) {
        // 构建微信支付链接
        // 实际实现需要使用微信支付SDK
        // 这里使用配置参数生成支付链接
        int totalFee = paymentOrder.getAmount().multiply(new BigDecimal(100)).intValue();
        String notifyUrl = callbackDomain + "/api/payment/callback/wechat";
        
        // 实际项目中应使用微信支付SDK生成签名和支付链接
        // 这里模拟生成支付链接，实际项目中应集成微信支付SDK
        return "weixin://wxpay/bizpayurl?appid=" + wechatAppId + "&mch_id=" + wechatMchId + "&nonce_str=" + System.currentTimeMillis() + "&sign=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX&body=支付订单&out_trade_no=" + paymentOrder.getOrderNo() + "&total_fee=" + totalFee + "&spbill_create_ip=127.0.0.1&notify_url=" + notifyUrl + "&trade_type=NATIVE";
    }
}