package com.alikeyou.itmodulepayment.util;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayFundTransUniTransferRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayFundTransUniTransferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.math.BigDecimal;

@Component
public class PayUtil {
    private static final Logger logger = LoggerFactory.getLogger(PayUtil.class);

    //appid
    private final String APP_ID = "9021000162646161";
    //应用私钥
    private final String APP_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCUXR96qWBck4HBy2mtKvdTfNsUqxse537ODTCoEPTv1t4OgqqIe/jjOje5u1lM5d7Xa3XXkGrs7oku2rXXV+Q8RnIpYiarmRee1Dl/31J38JIcM0GSlwAhS6jvjp81YRdqCXwyQWJ+79bTuH1dAEDsUJ9AtNM6Fd6r/LwWCZWs9kR5W7UD6GqdNItMJErWKs/moJv3q/qDjJ2MV7GCUguxR8rYRmSBa27YKQbIhhmc46dxSmvjveykjnUcr8kspKoBwDdAQrgRSJ1lxUuvTrl69aYx04n97LLDBvnt6fs6dWb1d8IjdKzSLFCvSnTmGhG2FLGy06l3YwX9YFRvAYI5AgMBAAECggEANYe7+efu6rgWEzQvdtmPz0G3UifCa04fRH0h1yH3HioqiJXAren9DQ9OV+cu+z3dP2B4bkSfL8mJGayz+3Ss1sqcGi5dwYBFM3tjG64Z9UiJjz83I4rz8w93bqfI7duQNSQi+Yb54M7sdguUv999jUYxj4kuxUBqzSctPc5LgNH1lB1xtWwrjb1oJ06oW+IN2k8QpFRksUAbrnFyOrs2Tn7iEvpAJXrgzEA6KRET3NzZI8Hg9DrSuAhNQiuPOd6y7u/DkxwUhrQi0lCbqe/s0HVffGSBZlf9Uguhd/8MWVIyCZN4lKVplGzszNL9Z3u+5DOWQWPnZTmyDu8wkHZQAQKBgQDDpBhFv7WsaV/cTs66cTwta7C+gOw1MpZZmSrgw/Vtci9MCHAXZucxuFuXn+JLiLlQjW5Z+Cd3tijdKxoPYL4iNFZL+0kvIX0/m9/IW9gc50Kzi81Nx8x0sTXLqgFWCw4RZOOM3iNSjVHbUVmYcyGLWXL+bsNJ3vu4Y8IFvLbtAQKBgQDCIwdkwWU/iPsCj7KhiZBnquNGSotyXls40H8yWiMvFalCW+etF8yDM6snTRFmpPiU2oQsh7/ePaJ9FtcfnRwcu9L+fNIhg84fE9jCgLNGPlICXvXTHyMrUWacgNV6lx0+4Ay5so5H6MJH4Jh46A5j2RYUISh24aSQ/4YxFE29OQKBgFuUduA/i3V3pi3knhTcaMUCQiwLAYctdP/gGRg54qsm5kj7Mi1iOBsoJ4fkifO9vrGcYumuphr9pEdTo9FLAfXU9gh/SpTLYmviNXD+vT1aa/jMGoCsZGLDiWEa5aE3b5Bo37PTUEmWaZj2jbA1RKCMcK31f2kW/lIkMHCrFB4BAoGAe1we2ps5ZwH33kzJSXRMWlNwT74Wlfn3JUb3TFuSd9zfzNbxhD/XKJewbiy+nea+PngL/O/Vr7aihbH5yJDgyDFhhWxzafCDDaRi3FVDlaMMIY2NzzW990ymU1RaUqerYcSk0WrwlikFKv9AY7OxUSkkqm3XdJM1Kx3ze0RXkaECgYEAtXV1VQYOox0nBN6edDDro34pBw/UwNdYJNf1YDSR+jiqvXLD3S4vfKXZNWtj5eM7UBXesbsAnZptnYJZgd3vl3DqmTJ9KcPpQWtrS7Jha/uRH4POUvHsFolEIPAZzZnuvWkv5jNdHggDttxMciA9kDlzadaUzMI8EF+Oqb3rVG8=";
    private final String CHARSET = "UTF-8";
    // 支付宝公钥
    private final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhldggc+n3Fv/UPHjAgBhYW0zO/H/qsZ9J9xrgpysyv+l7TGGvFb/2Y3t6Fzw7jJbTE59h5iN+pu8X7AXyOV0OLgyKt3v7OCf0Bph58TwlFxRbIXNcDRofSlTINzfmpNQKdjfjRcGi+6SB9u7wmqexjqN46NKorBs55CXcsQJVABKhP5b+iIRAauxhTdDiL9Z3oQ9Pk/1f6kbXQKNNLp+Y8uMKPcNObYOmA3O3xG/aIYH+rlW6F7JspHQXBheCgbdvOwTz/ewh7Uh3DfMqHSXrHtfAOZJcPcuiQedAcphH0lwQ6x6Aq5Cv1cESk8coZpKuBGikxatzEt9ws/qy3ZyRwIDAQAB";
    //网关地址
    private final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private final String FORMAT = "JSON";
    //签名方式
    private final String SIGN_TYPE = "RSA2";
    //支付宝异步通知路径,付款完毕后会异步调用本项目的方法,必须为公网地址
    private final String NOTIFY_URL = "http://eaf69956.natappfree.cc/api/alipay/notify";
    //支付宝同步通知路径,也就是当付款完毕后跳转本项目的页面,可以不是公网地址
    private final String RETURN_URL = "http://localhost:3000/wallet";
    /**
     * 发送支付请求到支付宝
     * @param outTradeNo 商户订单号
     * @param totalAmount 订单总金额
     * @param subject 订单标题
     * @return 支付宝返回的HTML表单
     */
    public String sendRequestToAlipay(String outTradeNo, Float totalAmount, String subject) throws AlipayApiException {
        //获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(GATEWAY_URL, APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);

        //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(RETURN_URL);
        alipayRequest.setNotifyUrl(NOTIFY_URL);

        //商品描述（可空）
        String body = "";
        alipayRequest.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\","
                + "\"total_amount\":\"" + totalAmount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        //请求
        String result = alipayClient.pageExecute(alipayRequest).getBody();
        logger.info("支付宝支付请求成功，订单号: {}", outTradeNo);
        return result;
    }

        /**
     * 通过订单编号查询交易状态
     * @param outTradeNo 商户订单号
     * @return 支付宝返回的交易状态信息
     */
    public String query(String outTradeNo){
        if (outTradeNo == null || outTradeNo.trim().isEmpty()) {
            logger.error("查询订单状态时订单号为空");
            return null;
        }
        
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(getAlipayConfig());
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", outTradeNo);
            request.setBizContent(bizContent.toString());
            
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            String body = response.getBody();
            
            if (response.isSuccess()) {
                logger.info("查询订单状态成功，订单号: {}", outTradeNo);
            } else {
                logger.warn("查询订单状态失败，订单号: {}, 错误码: {}, 错误信息: {}", 
                    outTradeNo, response.getSubCode(), response.getSubMsg());
            }
            
            return body;
        } catch (AlipayApiException e) {
            logger.error("查询订单状态异常，订单号: {}", outTradeNo, e);
            return null;
        }
    }

    public AlipayConfig getAlipayConfig() {
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(GATEWAY_URL);
        alipayConfig.setAppId(APP_ID);
        alipayConfig.setPrivateKey(APP_PRIVATE_KEY);
        alipayConfig.setFormat(FORMAT);
        alipayConfig.setAlipayPublicKey(ALIPAY_PUBLIC_KEY);
        alipayConfig.setCharset(CHARSET);
        alipayConfig.setSignType(SIGN_TYPE);
        return alipayConfig;
    }

    /**
     * 支付宝单笔转账到个人账户（用于提现）
     * 使用沙箱环境进行测试
     * 
     * @param outBizNo 商户订单号，全局唯一
     * @param payeeAccount 收款方支付宝账号（沙箱账号）
     * @param payeeName 收款方真实姓名（沙箱环境必填）
     * @param amount 转账金额
     * @param remark 转账备注
     * @return 转账结果信息
     * @throws AlipayApiException 支付宝API异常
     */
    public String transferToAlipay(String outBizNo, String payeeAccount, String payeeName, BigDecimal amount, String remark) throws AlipayApiException {
        logger.info("开始调用支付宝转账接口，订单号: {}, 收款账号: {}, 姓名: {}, 金额: {}", outBizNo, payeeAccount, payeeName, amount);
        
        // 获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(getAlipayConfig());
        
        // 构造请求参数
        AlipayFundTransUniTransferRequest request = new AlipayFundTransUniTransferRequest();
        
        // 构建业务参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_biz_no", outBizNo); // 商户订单号
        bizContent.put("trans_amount", amount.toString()); // 转账金额
        bizContent.put("product_code", "TRANS_ACCOUNT_NO_PWD"); // 销售产品码
        bizContent.put("biz_scene", "DIRECT_TRANSFER"); // 业务场景
        
        // 收款方信息
        JSONObject payeeInfo = new JSONObject();
        payeeInfo.put("identity", payeeAccount); // 收款方支付宝账号
        payeeInfo.put("identity_type", "ALIPAY_LOGON_ID"); // 标识类型
        // 沙箱环境必须提供收款人姓名，生产环境可选
        if (payeeName != null && !payeeName.isEmpty()) {
            payeeInfo.put("name", payeeName); // 收款方姓名
        }
        bizContent.put("payee_info", payeeInfo);
        
        // 订单标题和备注
        bizContent.put("order_title", "创作者提现");
        if (remark != null && !remark.isEmpty()) {
            bizContent.put("remark", remark);
        }
        
        request.setBizContent(bizContent.toString());
        
        // 执行请求
        AlipayFundTransUniTransferResponse response = alipayClient.execute(request);
        
        if (response.isSuccess()) {
            logger.info("支付宝转账成功，订单号: {}, 响应: {}", outBizNo, response.getBody());
            return response.getBody();
        } else {
            logger.error("支付宝转账失败，订单号: {}, 错误码: {}, 错误信息: {}", 
                outBizNo, response.getSubCode(), response.getSubMsg());
            throw new AlipayApiException(response.getSubCode(), response.getSubMsg());
        }
    }
}
