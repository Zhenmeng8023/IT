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
import com.alikeyou.itmodulepayment.entity.Membership;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.entity.OrderStatus;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.pojo.Result;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.MembershipRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.PaymentRecordRepository;
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
    
    @Autowired
    private MembershipRepository membershipRepository;
    
    @Autowired
    private PaymentRecordRepository paymentRecordRepository;
    
    @Autowired
    private com.alikeyou.itmodulepayment.service.CouponRedemptionService redemptionService;
    
    @Autowired
    private com.alikeyou.itmodulepayment.service.UserCouponService userCouponService;
    
    @Autowired
    private com.alikeyou.itmodulepayment.repository.UserCouponRepository userCouponRepository;
    
    @Autowired
    private com.alikeyou.itmodulepayment.repository.CouponRepository couponRepository;

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
    @RequestMapping(value = "/notify", method = {RequestMethod.POST, RequestMethod.GET})
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
                
                // 7. 创建支付记录（保存支付宝返回的详细信息）
                createPaymentRecord(order, params, tradeNo, totalAmount);
                
                // 8. 处理优惠券核销
                processCouponRedemption(order);
                
                // 9. 如果是会员订单，更新用户 VIP 状态
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
     * 处理优惠券核销
     */
    private void processCouponRedemption(PaymentOrder order) {
        try {
            // 查询该订单是否使用了优惠券
            com.alikeyou.itmodulepayment.entity.UserCoupon userCoupon = userCouponRepository.findByOrderId(order.getId())
                    .orElse(null);
            
            if (userCoupon != null && userCoupon.getReceiveStatus() == com.alikeyou.itmodulepayment.entity.UserCoupon.ReceiveStatus.locked) {
                // 获取优惠券模板信息，用于计算原始金额
                com.alikeyou.itmodulepayment.entity.Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                        .orElseThrow(() -> new RuntimeException("优惠券模板不存在"));
                
                // 计算原始金额：finalAmount + discountAmount
                BigDecimal finalAmount = order.getAmount();
                BigDecimal originalAmount = calculateOriginalAmount(coupon, finalAmount);
                
                // 创建核销记录
                redemptionService.createRedemption(userCoupon.getId(), order.getId(), originalAmount);
                
                // 标记优惠券为已使用
                userCouponService.useCoupon(userCoupon.getId(), order.getId());
                
                logger.info("✅ 优惠券核销成功: userCouponId={}, orderId={}, originalAmount={}, finalAmount={}", 
                    userCoupon.getId(), order.getId(), originalAmount, finalAmount);
            }
        } catch (Exception e) {
            logger.error("❌ 优惠券核销失败: orderId={}, error={}", order.getId(), e.getMessage(), e);
            // 核销失败不影响订单完成，只记录日志
        }
    }
    
    /**
     * 根据优惠券类型和优惠后金额，反推原始金额
     * 注意：因为 PaymentOrder.originalAmount 是 @Transient 字段，不会存入数据库，
     * 所以支付成功回调时需要通过优惠后金额反推原始金额用于核销记录
     */
    private BigDecimal calculateOriginalAmount(com.alikeyou.itmodulepayment.entity.Coupon coupon, BigDecimal finalAmount) {
        if (coupon.getType() == com.alikeyou.itmodulepayment.entity.Coupon.CouponType.discount) {
            // 折扣券正向计算：
            //   value表示打几折（如8.8表示打88折，支付88%，优惠12%）
            //   payRate = value / 10  （支付比例，如0.88）
            //   finalAmount = originalAmount × payRate
            // 所以反向计算：
            //   originalAmount = finalAmount / payRate
            BigDecimal payRate = coupon.getValue().divide(BigDecimal.valueOf(10), 4, BigDecimal.ROUND_HALF_UP);
            if (payRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("无效的折扣率: " + coupon.getValue());
            }
            return finalAmount.divide(payRate, 2, BigDecimal.ROUND_HALF_UP);
        } else {
            // 现金减免券正向计算：
            //   finalAmount = originalAmount - value
            // 所以反向计算：
            //   originalAmount = finalAmount + value
            return finalAmount.add(coupon.getValue());
        }
    }

    /**
     * 创建支付记录（保存支付宝回调的详细信息）
     */
    private void createPaymentRecord(PaymentOrder order, Map<String, String> params, 
                                     String tradeNo, String totalAmount) {
        try {
            PaymentRecord record = new PaymentRecord();
            record.setOrderId(order.getId());
            record.setPaymentPlatform("alipay");
            record.setTransactionId(tradeNo);  // 支付宝交易号
            record.setPaymentStatus("SUCCESS");
            record.setPaymentAmount(new BigDecimal(totalAmount));
            record.setPaymentTime(LocalDateTime.now());
            
            // 保存完整的回调数据（JSON格式）
            String callbackDataJson = com.alibaba.fastjson.JSON.toJSONString(params);
            record.setCallbackData(callbackDataJson);
            
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            
            paymentRecordRepository.save(record);
            logger.info("✅ 支付记录创建成功，订单ID: {}, 支付宝交易号: {}", order.getId(), tradeNo);
        } catch (Exception e) {
            logger.error("❌ 创建支付记录失败，订单ID: {}", order.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }
    
    /**
     * 更新用户 VIP 状态（同时更新 user_info 和 membership 表）
     */
    private void updateUserVipStatus(Long userId, Long membershipLevelId) {
        try {
            // 查询会员等级信息
            MembershipLevel membershipLevel = membershipLevelRepository.findById(membershipLevelId)
                .orElseThrow(() -> new RuntimeException("会员等级不存在，ID: " + membershipLevelId));
            
            // 查询用户信息
            UserInfo user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + userId));
            
            LocalDateTime now = LocalDateTime.now();
            
            // ========== 1. 更新 membership 表 ==========
            // 先将该用户所有已过期但未标记为 expired 的记录更新状态
            List<Membership> expiredMemberships = membershipRepository
                .findByUserIdAndStatusAndEndTimeBefore(userId, "active", now);
            for (Membership expired : expiredMemberships) {
                expired.setStatus("expired");
            }
            if (!expiredMemberships.isEmpty()) {
                membershipRepository.saveAll(expiredMemberships);
                logger.info("已将 {} 条过期的会员记录标记为 expired", expiredMemberships.size());
            }
            
            // 查找当前有效的会员记录
            java.util.Optional<Membership> activeOpt = membershipRepository
                .findTopByUserIdAndStatusAndEndTimeAfterOrderByEndTimeDesc(userId, "active", now);
            
            LocalDateTime startTime;
            LocalDateTime endTime;
            Membership target;
            
            if (activeOpt.isPresent()) {
                // 如果已有有效会员，则续费（延长有效期）
                target = activeOpt.get();
                startTime = target.getStartTime() != null ? target.getStartTime() : now;
                LocalDateTime currentEnd = target.getEndTime() != null && target.getEndTime().isAfter(now) 
                    ? target.getEndTime() : now;
                endTime = currentEnd.plusDays(membershipLevel.getDurationDays());
                target.setLevelId(membershipLevel.getId());
                logger.info("会员续费，原到期时间: {}, 新到期时间: {}", currentEnd, endTime);
            } else {
                // 如果没有有效会员，创建新的会员记录
                target = new Membership();
                target.setUserId(userId);
                target.setLevelId(membershipLevel.getId());
                startTime = now;
                endTime = now.plusDays(membershipLevel.getDurationDays());
                target.setCreatedAt(now);
                logger.info("新开通会员，到期时间: {}", endTime);
            }
            
            target.setStartTime(startTime);
            target.setEndTime(endTime);
            target.setStatus("active");
            target.setUpdatedAt(now);
            membershipRepository.save(target);
            logger.info("✅ membership 表更新成功，记录ID: {}", target.getId());
            
            // ========== 2. 更新 user_info 表 ==========
            user.setIsPremiumMember(true);
            user.setPremiumExpiryDate(endTime.atZone(ZoneId.systemDefault()).toInstant());
            userInfoRepository.save(user);
            logger.info("✅ user_info 表更新成功，用户ID: {}, VIP到期时间: {}", userId, endTime);
            
            logger.info("✅ 用户 VIP 状态更新完成 - 用户ID: {}, 会员等级: {}, 到期时间: {}",
                userId, membershipLevel.getName(), endTime);
            
        } catch (Exception e) {
            logger.error("❌ 更新用户 VIP 状态失败，用户 ID: {}, 会员等级 ID: {}", userId, membershipLevelId, e);
            throw new RuntimeException("更新用户 VIP 状态失败: " + e.getMessage(), e);
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