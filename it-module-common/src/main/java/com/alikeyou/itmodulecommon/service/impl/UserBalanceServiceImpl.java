package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.dto.UserBalanceDTO;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulecommon.service.UserBalanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 用户余额服务实现类
 */
@Service
public class UserBalanceServiceImpl implements UserBalanceService {
    
    private final UserInfoRepository userInfoRepository;
    
    public UserBalanceServiceImpl(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }
    
    @Override
    public UserBalanceDTO getUserBalance(Long userId) {
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        UserBalanceDTO dto = new UserBalanceDTO();
        dto.setUserId(userInfo.getId());
        dto.setUsername(userInfo.getUsername());
        dto.setBalance(userInfo.getBalance() != null ? userInfo.getBalance() : BigDecimal.ZERO);
        
        return dto;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal updateUserBalance(Long userId, BigDecimal amount, String reason) {
        if (amount == null) {
            throw new IllegalArgumentException("变动金额不能为空");
        }
        
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 初始化余额（如果为 null）
        BigDecimal currentBalance = userInfo.getBalance() != null 
                ? userInfo.getBalance() 
                : BigDecimal.ZERO;
        
        // 更新余额
        BigDecimal newBalance = currentBalance.add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("余额不足，当前余额：" + currentBalance + ", 尝试变动：" + amount);
        }
        
        userInfo.setBalance(newBalance);
        userInfoRepository.save(userInfo);
        
        return newBalance;
    }
}
