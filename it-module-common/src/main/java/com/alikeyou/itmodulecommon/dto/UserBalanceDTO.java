package com.alikeyou.itmodulecommon.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户余额信息 DTO
 */
@Data
public class UserBalanceDTO {
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 账户余额
     */
    private BigDecimal balance;
}
