package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.service.PaymentCallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment/callback")
public class PaymentCallbackController {

    private final PaymentCallbackService paymentCallbackService;

    public PaymentCallbackController(PaymentCallbackService paymentCallbackService) {
        this.paymentCallbackService = paymentCallbackService;
    }

    // 支付宝回调
    @PostMapping("/alipay")
    public ResponseEntity<String> alipayCallback(@RequestBody String callbackData) {
        paymentCallbackService.handleAlipayCallback(callbackData);
        return ResponseEntity.ok("success");
    }

    // 微信支付回调
    @PostMapping("/wechat")
    public ResponseEntity<String> wechatCallback(@RequestBody String callbackData) {
        paymentCallbackService.handleWechatCallback(callbackData);
        return ResponseEntity.ok("<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>");
    }


}