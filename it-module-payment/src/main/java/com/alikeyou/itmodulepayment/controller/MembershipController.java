package com.alikeyou.itmodulepayment.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.MembershipDTO;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.entity.OrderStatus;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.pojo.Result;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.service.MembershipService;
import com.alikeyou.itmodulepayment.util.PayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {
    private static final Logger logger = LoggerFactory.getLogger(MembershipController.class);

    private final MembershipService membershipService;
    
    @Autowired
    private PaymentOrderRepository paymentOrderRepository;
    
    @Autowired
    private MembershipLevelRepository membershipLevelRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private PayUtil payUtil;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    // 检查用户是否为会员
    @GetMapping("/check-vip/{userId}")
    public ResponseEntity<Map<String, Object>> checkUserIsVip(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        Boolean isVip = membershipService.checkUserIsVip(userId);
        response.put("isVip", isVip);
        response.put("userId", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 获取用户的会员信息
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserMembershipInfo(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        var membershipOpt = membershipService.getUserMembershipInfo(userId);
        
        if (membershipOpt.isPresent()) {
            MembershipDTO dto = membershipOpt.get();
            response.put("success", true);
            response.put("data", dto);
        } else {
            response.put("success", false);
            response.put("message", "未找到用户会员信息");
        }
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 获取用户当前有效的会员信息
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<Map<String, Object>> getUserActiveMembership(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        var membershipOpt = membershipService.getUserActiveMembership(userId);
        
        if (membershipOpt.isPresent()) {
            MembershipDTO dto = membershipOpt.get();
            response.put("success", true);
            response.put("data", dto);
        } else {
            response.put("success", false);
            response.put("message", "用户暂无有效会员");
        }
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * 使用支付宝购买 VIP 会员
     * @param userId 用户ID
     * @param membershipLevelId 会员等级ID
     * @return 支付宝支付表单HTML
     */
    @PostMapping("/buy-vip")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result buyVipWithAlipay(@RequestParam Long userId, 
                                    @RequestParam Long membershipLevelId) {
        logger.info("收到VIP购买请求，用户ID: {}, 会员等级ID: {}", userId, membershipLevelId);
        
        try {
            // 1. 参数校验
            if (userId == null || membershipLevelId == null) {
                return Result.error("用户ID和会员等级ID不能为空");
            }
            
            // 2. 验证用户是否存在
            UserInfo user = userInfoRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + userId));
            
            // 3. 验证会员等级是否存在且可用
            MembershipLevel membershipLevel = membershipLevelRepository.findById(membershipLevelId)
                    .orElseThrow(() -> new RuntimeException("会员等级不存在，ID: " + membershipLevelId));
            
            if (!Boolean.TRUE.equals(membershipLevel.getIsEnabled())) {
                return Result.error("该会员等级已停用");
            }
            
            // 4. 生成订单号
            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String random = UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 8);
            String orderNo = "VIP" + time + random;
            
            // 5. 创建支付订单
            PaymentOrder order = new PaymentOrder();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setType("membership");
            order.setMembershipLevelId(membershipLevelId);
            order.setAmount(membershipLevel.getPrice());
            order.setPaymentMethod("alipay");
            order.setStatus(OrderStatus.PENDING.name());
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            
            paymentOrderRepository.save(order);
            logger.info("VIP订单创建成功，订单号: {}, 金额: {}", orderNo, membershipLevel.getPrice());
            
            // 6. 调用支付宝接口生成支付表单
            BigDecimal amount = membershipLevel.getPrice();
            String subject = "VIP会员 - " + membershipLevel.getName() + " (" + membershipLevel.getDurationDays() + "天)";
            
            String alipayForm = payUtil.sendRequestToAlipay(
                orderNo, 
                amount.floatValue(), 
                subject
            );
            
            logger.info("支付宝支付表单生成成功，订单号: {}", orderNo);
            
            // 7. 返回支付表单
            Map<String, Object> data = new HashMap<>();
            data.put("orderNo", orderNo);
            data.put("amount", amount);
            data.put("paymentForm", alipayForm);
            
            return Result.success(data);
            
        } catch (AlipayApiException e) {
            logger.error("调用支付宝接口失败", e);
            return Result.error("调用支付宝接口失败: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("VIP购买业务异常", e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("VIP购买系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }
}
