package com.alikeyou.itmodulecommon.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户余额充值/扣减请求 DTO
 */
@Data
public class UserBalanceRequest {
    
    /**
     * 变动金额（正数为充值，负数为扣减）
     */
    private BigDecimal amount;
    
    /**
     * 变动原因描述
     */
    private String reason;
}
