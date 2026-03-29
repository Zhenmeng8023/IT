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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

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
    
    // 测试用个人微信收款码 URL（使用本地图片）
    private static final String TEST_WECHAT_QRCODE_URL = "http://localhost:3000/about.png";

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
            logger.info("微信支付链接生成成功，订单号：{}", paymentOrder.getOrderNo());
            return url;
        } else {
            logger.error("不支持的支付方式：{}", paymentMethod);
            throw new RuntimeException("不支持的支付方式，仅支持支付宝（alipay）和微信支付（wechat）");
        }
    }

    private String generateAlipayUrl(PaymentOrder paymentOrder) {
        logger.info("开始生成支付宝支付链接，订单号：{}", paymentOrder.getOrderNo());
        
        String subject = "支付订单";
        if (paymentOrder.getType().equals("PAID_CONTENT")) {
            subject = "购买付费内容";
        } else if (paymentOrder.getType().equals("MEMBERSHIP")) {
            subject = "购买会员";
        }
        
        // 检查是否是本地环境配置
        boolean isLocalEnvironment = callbackDomain.contains("localhost") || callbackDomain.contains("127.0.0.1");
        
        String notifyUrl;
        String returnUrl;
        
        if (isLocalEnvironment) {
            // 本地开发环境：异步通知地址可以暂时留空或使用 ngrok
            // 如果不需要异步通知，可以设置为空字符串
            notifyUrl = ""; // 暂时不设置异步通知
            returnUrl = "http://localhost:3000/payment/success?orderNo=" + paymentOrder.getOrderNo();
            logger.info("本地环境配置：notifyUrl={}, returnUrl={}", notifyUrl, returnUrl);
        } else {
            notifyUrl = callbackDomain + "/api/payment/callback/alipay";
            returnUrl = callbackDomain + "/payment/success";
            logger.info("生产环境配置：notifyUrl={}, returnUrl={}", notifyUrl, returnUrl);
        }
        
        try {
            // 使用支付宝 SDK 生成页面支付 URL
            logger.info("========== 开始生成支付宝支付页面 ==========");
            logger.info("APPID: {}", alipayAppId);
            logger.info("私钥长度：{}", alipayPrivateKey != null ? alipayPrivateKey.length() : 0);
            logger.info("公钥长度：{}", alipayPublicKey != null ? alipayPublicKey.length() : 0);
            
            // 初始化支付宝客户端
            com.alipay.api.AlipayClient alipayClient = new com.alipay.api.DefaultAlipayClient(
                "https://openapi.alipaydev.com/gateway.do",  // 沙箱环境网关
                alipayAppId,                                   // 应用 ID
                alipayPrivateKey,                              // 应用私钥
                "json",                                        // 数据格式
                "utf-8",                                       // 字符集
                alipayPublicKey,                               // 支付宝公钥
                "RSA2"                                         // 签名类型
            );
            logger.info("支付宝客户端初始化成功");
            
            // 构建支付宝页面支付请求
            com.alipay.api.request.AlipayTradePagePayRequest request = new com.alipay.api.request.AlipayTradePagePayRequest();
            request.setReturnUrl(returnUrl);      // 同步回调地址
            request.setNotifyUrl(notifyUrl);      // 异步通知地址
            
            // 业务参数
            String bizContent = "{" +
                "\"out_trade_no\":\"" + paymentOrder.getOrderNo() + "\"," +
                "\"total_amount\":" + paymentOrder.getAmount() + "," +
                "\"subject\":\"" + subject + "\"," +
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"" +
                "}";
            logger.info("业务参数：{}", bizContent);
            request.setBizContent(bizContent);
            
            logger.info("开始调用支付宝 API 生成支付页面...");
            // 生成支付页面 URL（表单形式）
            String form = alipayClient.pageExecute(request).getBody();
            logger.info("支付宝支付页面生成成功，订单号：{}", paymentOrder.getOrderNo());
            logger.info("========================================");
            
            // 返回一个 HTML 表单，前端会自动提交
            return form;
            
        } catch (Exception e) {
            logger.error("生成支付宝支付页面失败", e);
            throw new RuntimeException("生成支付宝支付页面失败：" + e.getMessage(), e);
        }
    }

    private String generateWechatUrl(PaymentOrder paymentOrder) {
        logger.info("开始生成微信支付链接，订单号：{}", paymentOrder.getOrderNo());
        
        // 将金额转换为分
        int totalFee = paymentOrder.getAmount().multiply(new BigDecimal(100)).intValue();
        String notifyUrl = callbackDomain + "/api/payment/callback/wechat";
        
        // 检查是否为测试环境配置
        boolean isTestConfig = "wx0000000000000000".equals(wechatAppId) || 
                               "1000000000".equals(wechatMchId) || 
                               "your-wechat-api-key".equals(wechatApiKey);
        
        logger.info("微信支付配置检查 - appid: {}, mch_id: {}, api-key 是否为默认：{}", 
            wechatAppId, wechatMchId, "your-wechat-api-key".equals(wechatApiKey));
        logger.info("是否为测试配置：{}", isTestConfig);
        
        // 注意：如果要使用真实微信支付，请在 application.properties 中配置真实的参数
        // 并删除或修改上面的测试配置判断
        if (isTestConfig) {
            logger.warn("检测到测试环境配置，返回模拟支付二维码页面 URL");
            // 测试环境：返回一个前端页面 URL（注意：前端运行在 3000 端口）
            // 使用个人微信收款码 URL
            String testCodeUrl = TEST_WECHAT_QRCODE_URL;
            String testUrl = "http://localhost:3000/payment?orderNo=" + paymentOrder.getOrderNo() + 
                   "&amount=" + paymentOrder.getAmount() + "&type=wechat" +
                   "&codeUrl=" + java.net.URLEncoder.encode(testCodeUrl, StandardCharsets.UTF_8);
            logger.info("返回测试 URL: {}", testUrl);
            return testUrl;
        }
        
        // 生产环境：调用微信支付统一下单接口（需要真实配置）
        Map<String, String> unifiedOrderParams = new HashMap<>();
        unifiedOrderParams.put("appid", wechatAppId);
        unifiedOrderParams.put("mch_id", wechatMchId);
        unifiedOrderParams.put("nonce_str", System.currentTimeMillis() + "");
        unifiedOrderParams.put("body", "支付订单 - " + paymentOrder.getType());
        unifiedOrderParams.put("out_trade_no", paymentOrder.getOrderNo());
        unifiedOrderParams.put("total_fee", totalFee + "");
        unifiedOrderParams.put("spbill_create_ip", "127.0.0.1");
        unifiedOrderParams.put("notify_url", notifyUrl);
        unifiedOrderParams.put("trade_type", "NATIVE");
        
        // 生成签名
        String sign = generateWechatSign(unifiedOrderParams);
        unifiedOrderParams.put("sign", sign);
        
        try {
            logger.debug("调用微信支付统一下单接口，参数：{}", unifiedOrderParams);
            String xmlRequest = mapToXml(unifiedOrderParams);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.mch.weixin.qq.com/pay/unifiedorder"))
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(xmlRequest))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("微信支付统一下单响应状态码：{}", response.statusCode());
            Map<String, String> result = parseXml(response.body());
            
            // 检查返回结果
            if (result == null || result.isEmpty()) {
                logger.error("微信支付统一下单返回为空");
                throw new RuntimeException("微信支付统一下单返回为空");
            }
            
            if ("SUCCESS".equals(result.get("return_code")) && "SUCCESS".equals(result.get("result_code"))) {
                String codeUrl = result.get("code_url");
                if (codeUrl == null || codeUrl.isEmpty()) {
                    logger.error("微信统一下单成功，但未返回 code_url");
                    throw new RuntimeException("微信统一下单成功，但未返回 code_url");
                }
                logger.info("微信统一下单成功，获取到 code_url: {}", codeUrl);
                
                // 不直接返回 code_url（会打开微信），而是返回一个显示二维码的前端页面 URL
                // 前端会根据这个 code_url 生成二维码图片
                String frontendPaymentPage = "http://localhost:3000/payment?orderNo=" + paymentOrder.getOrderNo() + 
                                             "&amount=" + paymentOrder.getAmount() + "&type=wechat" +
                                             "&codeUrl=" + java.net.URLEncoder.encode(codeUrl, StandardCharsets.UTF_8);
                logger.info("生成微信支付二维码页面：{}", frontendPaymentPage);
                return frontendPaymentPage;
            } else {
                String errorMsg = result.get("return_msg") != null ? result.get("return_msg") : 
                                 (result.get("err_code_des") != null ? result.get("err_code_des") : "未知错误");
                logger.error("微信统一下单失败：return_code={}, result_code={}, message={}", 
                    result.get("return_code"), result.get("result_code"), errorMsg);
                throw new RuntimeException("微信统一下单失败：" + errorMsg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("调用微信支付统一下单接口被中断", e);
            throw new RuntimeException("调用微信支付统一下单接口被中断：" + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("调用微信支付统一下单接口失败", e);
            throw new RuntimeException("调用微信支付统一下单接口失败：" + e.getMessage(), e);
        }
    }

    // 辅助方法：生成微信支付签名
    private String generateWechatSign(Map<String, String> params) {
        // 实际项目中应使用微信支付 SDK 的签名方法
        // 例如：WXPayUtil.generateSignature(params, wechatApiKey)
        // 这里仅做示例
        StringBuilder sb = new StringBuilder();
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            if (!"sign".equals(key) && params.get(key) != null && !"".equals(params.get(key))) {
                sb.append(key).append("=").append(params.get(key)).append("&");
            }
        }
        sb.append("key=").append(wechatApiKey);
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("生成 MD5 签名失败", e);
        }
    }

    // 辅助方法：Map 转 XML
    private String mapToXml(Map<String, String> params) throws Exception {
        StringBuilder xml = new StringBuilder("<xml>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            xml.append("<").append(entry.getKey()).append(">")
               .append(entry.getValue())
               .append("</").append(entry.getKey()).append(">");
        }
        xml.append("</xml>");
        return xml.toString();
    }

    // 辅助方法：解析 XML
    private Map<String, String> parseXml(String xml) throws Exception {
        Map<String, String> map = new HashMap<>();
        org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        org.w3c.dom.NodeList nodeList = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            org.w3c.dom.Node node = nodeList.item(i);
            if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                map.put(node.getNodeName(), node.getTextContent());
            }
        }
        return map;
    }
}