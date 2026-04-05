package com.alikeyou.itmodulepayment.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.PayRequest;
import com.alikeyou.itmodulepayment.dto.RefundRequest;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.entity.OrderStatus;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.pojo.Result;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.util.PayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/api/alipay")
public class AliPayController {
    private static final Logger logger = LoggerFactory.getLogger(AliPayController.class);

    @Autowired
    private PayUtil payUtil;
    
    @Autowired
    private PaymentOrderRepository paymentOrderRepository;
    
    @Autowired
    private MembershipLevelRepository membershipLevelRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;


    @ResponseBody
    @PostMapping("/pay")
    @Transactional(rollbackFor = Exception.class)
    public Result alipay(@RequestBody PayRequest payRequest) throws AlipayApiException {
        logger.info("收到支付宝支付请求: {}", payRequest);

        try {
            // 参数校验
            if (payRequest.getOrderIds() == null || payRequest.getOrderIds().isEmpty()) {
                return Result.error("订单ID列表不能为空");
            }
            if (payRequest.getValue() == null || payRequest.getValue().trim().isEmpty()) {
                return Result.error("支付金额不能为空");
            }

            //生成订单号（支付宝的要求）
            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String user = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            List<Long> orderIds = payRequest.getOrderIds();
            String payId = time + user;
            
            // TODO: 这里需要实现实际的订单处理逻辑
            // 目前 CartService 和 OrderService 不存在，需要根据实际业务逻辑调整
            logger.info("生成支付订单号: {}, 订单IDs: {}", payId, orderIds);
            
            for (Long orderId : orderIds) {
                // TODO: 实现订单状态更新逻辑
                logger.info("处理订单: {}", orderId);
            }

            float value = Float.parseFloat(payRequest.getValue());
            //调用封装好的方法（给支付宝接口发送请求）
            String content = payUtil.sendRequestToAlipay(payId, value, "支付数据");
            
            logger.info("支付宝支付表单生成成功");
            return Result.success(content);
            
        } catch (NumberFormatException e) {
            logger.error("支付金额格式错误: {}", payRequest.getValue(), e);
            return Result.error("支付金额格式错误");
        } catch (AlipayApiException e) {
            logger.error("调用支付宝接口失败", e);
            return Result.error("调用支付宝接口失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("支付处理异常", e);
            throw e; // 抛出异常以触发事务回滚
        }
    }


    

    /**
     * 支付宝异步通知回调（重要！）
     * 支付宝服务器会在支付成功后调用此接口
     * 必须返回 "success" 字符串，否则支付宝会重复通知
     */
    @PostMapping("/notify")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public String alipayNotify(@RequestParam Map<String, String> params) {
        logger.info("收到支付宝异步通知，参数: {}", params);
        
        try {
            // 1. 验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(
                params,
                payUtil.getAlipayConfig().getAlipayPublicKey(),
                payUtil.getAlipayConfig().getCharset(),
                payUtil.getAlipayConfig().getSignType()
            );
            
            if (!signVerified) {
                logger.error("支付宝异步通知签名验证失败");
                return "failure";
            }
            
            // 2. 获取关键参数
            String outTradeNo = params.get("out_trade_no");  // 商户订单号
            String tradeNo = params.get("trade_no");          // 支付宝交易号
            String tradeStatus = params.get("trade_status");  // 交易状态
            String totalAmount = params.get("total_amount");  // 订单金额
            
            logger.info("支付宝异步通知 - 订单号: {}, 交易号: {}, 状态: {}, 金额: {}",
                outTradeNo, tradeNo, tradeStatus, totalAmount);
            
            // 3. 判断交易状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 4. 查询订单
                PaymentOrder order = paymentOrderRepository.findByOrderNo(outTradeNo)
                    .orElseThrow(() -> new RuntimeException("订单不存在，订单号: " + outTradeNo));
                
                // 5. 检查订单是否已处理（幂等性）
                if (OrderStatus.PAID.name().equals(order.getStatus())) {
                    logger.info("订单已处理，无需重复处理，订单号: {}", outTradeNo);
                    return "success";  // 已处理也返回 success
                }
                
                // 6. 更新订单状态
                order.setStatus(OrderStatus.PAID.name());
                order.setPayTime(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                paymentOrderRepository.save(order);
                logger.info("订单状态更新为已支付，订单号: {}", outTradeNo);
                
                // 7. 如果是会员订单，更新用户 VIP 状态
                if ("membership".equals(order.getType()) && order.getMembershipLevelId() != null) {
                    updateUserVipStatus(order.getUserId(), order.getMembershipLevelId());
                }
                
                logger.info("支付宝异步通知处理成功，订单号: {}", outTradeNo);
                return "success";  // 必须返回 success
            } else {
                logger.warn("支付宝交易状态不是成功状态: {}, 订单号: {}", tradeStatus, outTradeNo);
                return "failure";
            }
            
        } catch (Exception e) {
            logger.error("处理支付宝异步通知异常", e);
            return "failure";  // 返回 failure 让支付宝重试
        }
    }
    
    /**
     * 更新用户 VIP 状态
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
            Instant newExpiryTime;
            
            // 如果已有 VIP 且未过期，则在原有过期时间基础上延长
            if (Boolean.TRUE.equals(user.getIsPremiumMember()) && user.getPremiumExpiryDate() != null) {
                LocalDateTime currentExpiry = LocalDateTime.ofInstant(
                    user.getPremiumExpiryDate(), ZoneId.systemDefault());
                if (currentExpiry.isAfter(now)) {
                    newExpiryTime = currentExpiry.plusDays(membershipLevel.getDurationDays())
                        .atZone(ZoneId.systemDefault()).toInstant();
                } else {
                    newExpiryTime = now.plusDays(membershipLevel.getDurationDays())
                        .atZone(ZoneId.systemDefault()).toInstant();
                }
            } else {
                newExpiryTime = now.plusDays(membershipLevel.getDurationDays())
                    .atZone(ZoneId.systemDefault()).toInstant();
            }
            
            user.setPremiumExpiryDate(newExpiryTime);
            userInfoRepository.save(user);
            
            logger.info("✅ 用户 VIP 状态更新成功，用户 ID: {}, 会员等级: {}, 到期时间: {}",
                userId, membershipLevel.getName(), newExpiryTime);
            
        } catch (Exception e) {
            logger.error("❌ 更新用户 VIP 状态失败，用户 ID: {}, 会员等级 ID: {}", userId, membershipLevelId, e);
            throw e;  // 抛出异常触发事务回滚
        }
    }

    /**
     * 支付宝同步通知回调
     * 当支付完成之后跳转这个请求并携带参数，我们将里面的订单id接收到，通过订单id查询订单信息
     */
    @GetMapping("/toSuccess")
    public String returns(String out_trade_no) {
        logger.info("收到支付宝同步通知，订单号: {}", out_trade_no);
        
        if (out_trade_no == null || out_trade_no.trim().isEmpty()) {
            logger.error("订单号为空");
            return "redirect:" + frontendUrl + "/wallet?error=订单号不能为空";
        }
        
        try {
            // 查询订单状态（从数据库）
            PaymentOrder order = paymentOrderRepository.findByOrderNo(out_trade_no)
                .orElse(null);
            
            if (order != null && OrderStatus.PAID.name().equals(order.getStatus())) {
                // 订单已支付成功
                logger.info("订单已支付成功: {}", out_trade_no);
                return "redirect:" + frontendUrl + "/wallet?success=true&orderNo=" + out_trade_no;
            }
            
            // 如果订单还未支付，查询支付宝状态
            String query = payUtil.query(out_trade_no);
            
            if (query == null || query.trim().isEmpty()) {
                logger.error("查询支付宝交易状态返回为空");
                return "redirect:" + frontendUrl + "/wallet?error=查询交易状态失败";
            }
            
            logger.info("支付宝交易状态查询结果: {}", query);

            // 解析支付宝返回的JSON响应数据
            JSONObject jsonObject = JSONObject.parseObject(query);
            Object o = jsonObject.get("alipay_trade_query_response");
            
            if (o == null) {
                logger.error("支付宝响应中缺少 alipay_trade_query_response 字段");
                return "redirect:" + frontendUrl + "/wallet?error=响应数据格式错误";
            }
            
            Map map = (Map) o;
            Object s = map.get("trade_status");
            
            if ("TRADE_SUCCESS".equals(s) || "TRADE_FINISHED".equals(s)) {
                logger.info("订单支付成功: {}", out_trade_no);
                return "redirect:" + frontendUrl + "/wallet?success=true&orderNo=" + out_trade_no;
            } else {
                logger.warn("订单支付未完成，状态: {}, 订单号: {}", s, out_trade_no);
                return "redirect:" + frontendUrl + "/wallet?error=支付未完成，当前状态: " + s;
            }
        } catch (Exception e) {
            logger.error("处理支付宝同步通知异常，订单号: {}", out_trade_no, e);
            return "redirect:" + frontendUrl + "/wallet?error=处理支付结果异常";
        }
    }

    /**
     * 退款接口
     */
    @PostMapping("/refund")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result aliPayRefund(@RequestBody RefundRequest refundRequest) {
        logger.info("收到退款请求: {}", refundRequest);
        
        try {
            // 参数校验
            if (refundRequest.getPayId() == null || refundRequest.getPayId().trim().isEmpty()) {
                return Result.error("支付订单号不能为空");
            }
            if (refundRequest.getValue() == null || refundRequest.getValue().trim().isEmpty()) {
                return Result.error("退款金额不能为空");
            }
            
            // 初始化SDK
            AlipayClient alipayClient = new DefaultAlipayClient(payUtil.getAlipayConfig());

            // 构造请求参数以调用接口
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();

            // 设置商户订单号
            model.setOutTradeNo(refundRequest.getPayId());

            // 设置退款金额
            model.setRefundAmount(refundRequest.getValue());

            // 设置退款原因说明
            model.setRefundReason(refundRequest.getReason() != null ? refundRequest.getReason() : "正常退款");

            request.setBizModel(model);

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            logger.info("支付宝退款响应: {}", response.getBody());

            if (response.isSuccess()) {
                logger.info("支付宝退款调用成功，订单号: {}", refundRequest.getPayId());
                
                // TODO: 实现订单退款逻辑
                // orderService.refundOrder(refundRequest.getOrderId(), refundRequest.getReason());
                logger.info("订单退款处理完成，订单ID: {}", refundRequest.getOrderId());
                
                return Result.success("退款成功");
            } else {
                logger.error("支付宝退款调用失败，子码: {}, 子消息: {}", 
                    response.getSubCode(), response.getSubMsg());
                return Result.error("退款失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            logger.error("调用支付宝退款接口异常", e);
            return Result.error("调用支付宝退款接口失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("退款处理异常", e);
            throw e; // 抛出异常以触发事务回滚
        }
    }

}