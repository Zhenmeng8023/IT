package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.dto.UserBalanceDTO;

import java.math.BigDecimal;

/**
 * 用户余额服务接口
 */
public interface UserBalanceService {
    
    /**
     * 获取用户余额信息
     * @param userId 用户 ID
     * @return 用户余额信息
     */
    UserBalanceDTO getUserBalance(Long userId);
    
    /**
     * 更新用户余额
     * @param userId 用户 ID
     * @param amount 变动金额（正数为充值，负数为扣减）
     * @param reason 变动原因
     * @return 更新后的余额
     */
    BigDecimal updateUserBalance(Long userId, BigDecimal amount, String reason);
}
