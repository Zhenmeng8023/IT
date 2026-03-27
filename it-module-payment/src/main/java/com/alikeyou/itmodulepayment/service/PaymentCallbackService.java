package com.alikeyou.itmodulepayment.service;

public interface PaymentCallbackService {
    void handleAlipayCallback(String callbackData);
    void handleWechatCallback(String callbackData);
}