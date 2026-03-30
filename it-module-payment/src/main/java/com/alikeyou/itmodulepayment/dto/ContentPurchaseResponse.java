package com.alikeyou.itmodulepayment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContentPurchaseResponse {
    private Boolean success;
    private String message;
    private String orderNo;
    private BigDecimal amount;
    private Boolean alreadyPurchased; // 是否已购买
}
