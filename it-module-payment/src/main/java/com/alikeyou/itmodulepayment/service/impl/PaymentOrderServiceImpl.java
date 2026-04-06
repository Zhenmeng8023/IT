package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.PaymentOrderDTO;
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

    private static final String TEST_WECHAT_QRCODE_URL = "http://localhost:3000/about.png";

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
        String orderNo = "ORDER" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%06d", (int)(Math.random() * 1000000));
        paymentOrder.setOrderNo(orderNo);
        paymentOrder.setType(normalizeOrderType(paymentOrder.getType()));
        paymentOrder.setStatus("pending");
        paymentOrder.setCreatedAt(LocalDateTime.now());
        paymentOrder.setUpdatedAt(LocalDateTime.now());
        return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    @Transactional
    public PaymentOrder updateOrder(Long id, PaymentOrderDTO dto) {
        PaymentOrder paymentOrder = paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        BeanUtils.copyProperties(dto, paymentOrder);
        paymentOrder.setType(normalizeOrderType(paymentOrder.getType()));
        paymentOrder.setUpdatedAt(LocalDateTime.now());
        return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        paymentOrderRepository.deleteById(id);
    }

    @Override
    public PaymentOrder getOrderById(Long id) {
        return paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    @Override
    public PaymentOrder getOrderByOrderNo(String orderNo) {
        return paymentOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    @Override
    public List<PaymentOrder> getOrdersByUserId(Long userId) {
        return paymentOrderRepository.findByUserId(userId);
    }

    @Override
    public List<PaymentOrder> getOrdersByUserIdAndType(Long userId, String type) {
        return paymentOrderRepository.findByUserIdAndType(userId, normalizeOrderType(type));
    }

    @Override
    public List<PaymentOrder> getOrdersByStatus(String status) {
        return paymentOrderRepository.findByStatus(normalizeStatus(status));
    }

    @Override
    public List<PaymentOrder> getOrdersByPaidContentId(Long paidContentId) {
        return paymentOrderRepository.findByPaidContentId(paidContentId);
    }

    @Override
    public List<PaymentOrder> getOrdersByMembershipLevelId(Long membershipLevelId) {
        return paymentOrderRepository.findByMembershipLevelId(membershipLevelId);
    }

    @Override
    public String generatePaymentUrl(PaymentOrder paymentOrder, String paymentMethod) {
        if ("alipay".equals(paymentMethod)) {
            return generateAlipayUrl(paymentOrder);
        } else if ("wechat".equals(paymentMethod)) {
            return generateWechatUrl(paymentOrder);
        }
        throw new RuntimeException("不支持的支付方式，仅支持支付宝（alipay）和微信支付（wechat）");
    }

    private String generateAlipayUrl(PaymentOrder paymentOrder) {
        String subject = "支付订单";
        String type = normalizeOrderType(paymentOrder.getType());
        if ("content".equals(type)) {
            subject = "购买付费内容";
        } else if ("membership".equals(type)) {
            subject = "购买会员";
        }

        boolean isLocalEnvironment = callbackDomain.contains("localhost") || callbackDomain.contains("127.0.0.1");
        String notifyUrl;
        String returnUrl;
        if (isLocalEnvironment) {
            notifyUrl = "";
            returnUrl = "http://localhost:3000/payment/success?orderNo=" + paymentOrder.getOrderNo();
        } else {
            notifyUrl = callbackDomain + "/api/payment/callback/alipay";
            returnUrl = callbackDomain + "/payment/success";
        }

        try {
            com.alipay.api.AlipayClient alipayClient = new com.alipay.api.DefaultAlipayClient(
                "https://openapi.alipaydev.com/gateway.do",
                alipayAppId,
                alipayPrivateKey,
                "json",
                "utf-8",
                alipayPublicKey,
                "RSA2"
            );
            com.alipay.api.request.AlipayTradePagePayRequest request = new com.alipay.api.request.AlipayTradePagePayRequest();
            request.setReturnUrl(returnUrl);
            request.setNotifyUrl(notifyUrl);
            String bizContent = "{" +
                "\"out_trade_no\":\"" + paymentOrder.getOrderNo() + "\"," +
                "\"total_amount\":" + paymentOrder.getAmount() + "," +
                "\"subject\":\"" + subject + "\"," +
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"" +
                "}";
            request.setBizContent(bizContent);
            return alipayClient.pageExecute(request).getBody();
        } catch (Exception e) {
            throw new RuntimeException("生成支付宝支付页面失败：" + e.getMessage(), e);
        }
    }

    private String generateWechatUrl(PaymentOrder paymentOrder) {
        int totalFee = paymentOrder.getAmount().multiply(new BigDecimal(100)).intValue();
        String notifyUrl = callbackDomain + "/api/payment/callback/wechat";
        boolean isTestConfig = "wx0000000000000000".equals(wechatAppId) || "1000000000".equals(wechatMchId) || "your-wechat-api-key".equals(wechatApiKey);
        if (isTestConfig) {
            String testCodeUrl = TEST_WECHAT_QRCODE_URL;
            return "http://localhost:3000/payment?orderNo=" + paymentOrder.getOrderNo() +
                   "&amount=" + paymentOrder.getAmount() + "&type=wechat" +
                   "&codeUrl=" + java.net.URLEncoder.encode(testCodeUrl, StandardCharsets.UTF_8);
        }
        Map<String, String> unifiedOrderParams = new HashMap<>();
        unifiedOrderParams.put("appid", wechatAppId);
        unifiedOrderParams.put("mch_id", wechatMchId);
        unifiedOrderParams.put("nonce_str", System.currentTimeMillis() + "");
        unifiedOrderParams.put("body", "支付订单 - " + normalizeOrderType(paymentOrder.getType()));
        unifiedOrderParams.put("out_trade_no", paymentOrder.getOrderNo());
        unifiedOrderParams.put("total_fee", totalFee + "");
        unifiedOrderParams.put("spbill_create_ip", "127.0.0.1");
        unifiedOrderParams.put("notify_url", notifyUrl);
        unifiedOrderParams.put("trade_type", "NATIVE");
        String sign = generateWechatSign(unifiedOrderParams);
        unifiedOrderParams.put("sign", sign);
        try {
            String xmlRequest = mapToXml(unifiedOrderParams);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.mch.weixin.qq.com/pay/unifiedorder"))
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(xmlRequest))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, String> result = parseXml(response.body());
            if (result == null || result.isEmpty()) {
                throw new RuntimeException("微信支付统一下单返回为空");
            }
            if ("SUCCESS".equals(result.get("return_code")) && "SUCCESS".equals(result.get("result_code"))) {
                String codeUrl = result.get("code_url");
                if (codeUrl == null || codeUrl.isEmpty()) {
                    throw new RuntimeException("微信统一下单成功，但未返回 code_url");
                }
                return "http://localhost:3000/payment?orderNo=" + paymentOrder.getOrderNo() +
                       "&amount=" + paymentOrder.getAmount() + "&type=wechat" +
                       "&codeUrl=" + java.net.URLEncoder.encode(codeUrl, StandardCharsets.UTF_8);
            }
            String errorMsg = result.get("return_msg") != null ? result.get("return_msg") : (result.get("err_code_des") != null ? result.get("err_code_des") : "未知错误");
            throw new RuntimeException("微信统一下单失败：" + errorMsg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("调用微信支付统一下单接口被中断：" + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("调用微信支付统一下单接口失败：" + e.getMessage(), e);
        }
    }

    private String generateWechatSign(Map<String, String> params) {
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

    private String normalizeOrderType(String type) {
        String value = type == null ? "" : type.trim().toLowerCase();
        if ("paid_content".equals(value)) return "content";
        if ("membership".equals(value)) return "membership";
        if ("content".equals(value)) return "content";
        return value;
    }

    private String normalizeStatus(String status) {
        return status == null ? "pending" : status.trim().toLowerCase();
    }
}
