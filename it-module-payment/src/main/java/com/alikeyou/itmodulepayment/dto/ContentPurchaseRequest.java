package com.alikeyou.itmodulepayment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContentPurchaseRequest {
    private Long blogId;
    private String paymentMethod; // 支付方式：wechat, alipay, balance
}
