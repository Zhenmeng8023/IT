package com.alikeyou.itmodulepayment.service;

public interface PaymentCallbackService {
    String handleAlipayCallback(String callbackData);
    String handleWechatCallback(String callbackData);
}