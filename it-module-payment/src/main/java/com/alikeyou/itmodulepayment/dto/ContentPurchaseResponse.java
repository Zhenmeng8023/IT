package com.alikeyou.itmodulepayment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContentPurchaseResponse {
    private Boolean success;
    private String message;
    private String orderNo;
    private BigDecimal amount; // 实际支付金额（优惠后）
    private BigDecimal originalAmount; // 原价
    private BigDecimal discountAmount; // 优惠金额
    private Long userCouponId; // 使用的优惠券ID
    private Boolean alreadyPurchased; // 是否已购买
}
