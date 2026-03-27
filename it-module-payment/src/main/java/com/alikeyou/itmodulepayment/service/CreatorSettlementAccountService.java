package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.CreatorSettlementAccountDTO;
import com.alikeyou.itmodulepayment.entity.CreatorSettlementAccount;

import java.util.List;

public interface CreatorSettlementAccountService {

    // 创建创作者结算账户
    CreatorSettlementAccount createCreatorSettlementAccount(CreatorSettlementAccountDTO dto);

    // 更新创作者结算账户
    CreatorSettlementAccount updateCreatorSettlementAccount(Long id, CreatorSettlementAccountDTO dto);

    // 删除创作者结算账户
    void deleteCreatorSettlementAccount(Long id);

    // 根据ID查询创作者结算账户
    CreatorSettlementAccount getCreatorSettlementAccountById(Long id);

    // 根据用户ID查询创作者结算账户
    List<CreatorSettlementAccount> getCreatorSettlementAccountsByUserId(Long userId);

    // 根据用户ID和状态查询创作者结算账户
    List<CreatorSettlementAccount> getCreatorSettlementAccountsByUserIdAndStatus(Long userId, String status);
}