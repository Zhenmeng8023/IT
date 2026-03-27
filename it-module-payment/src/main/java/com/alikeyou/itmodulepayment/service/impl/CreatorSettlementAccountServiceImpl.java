package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.CreatorSettlementAccountDTO;
import com.alikeyou.itmodulepayment.entity.CreatorSettlementAccount;
import com.alikeyou.itmodulepayment.repository.CreatorSettlementAccountRepository;
import com.alikeyou.itmodulepayment.service.CreatorSettlementAccountService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreatorSettlementAccountServiceImpl implements CreatorSettlementAccountService {

    private final CreatorSettlementAccountRepository creatorSettlementAccountRepository;

    public CreatorSettlementAccountServiceImpl(CreatorSettlementAccountRepository creatorSettlementAccountRepository) {
        this.creatorSettlementAccountRepository = creatorSettlementAccountRepository;
    }

    @Override
    public CreatorSettlementAccount createCreatorSettlementAccount(CreatorSettlementAccountDTO dto) {
        CreatorSettlementAccount account = new CreatorSettlementAccount();
        BeanUtils.copyProperties(dto, account);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return creatorSettlementAccountRepository.save(account);
    }

    @Override
    public CreatorSettlementAccount updateCreatorSettlementAccount(Long id, CreatorSettlementAccountDTO dto) {
        CreatorSettlementAccount account = creatorSettlementAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("结算账户不存在"));
        BeanUtils.copyProperties(dto, account);
        account.setUpdatedAt(LocalDateTime.now());
        return creatorSettlementAccountRepository.save(account);
    }

    @Override
    public void deleteCreatorSettlementAccount(Long id) {
        creatorSettlementAccountRepository.deleteById(id);
    }

    @Override
    public CreatorSettlementAccount getCreatorSettlementAccountById(Long id) {
        return creatorSettlementAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("结算账户不存在"));
    }

    @Override
    public List<CreatorSettlementAccount> getCreatorSettlementAccountsByUserId(Long userId) {
        return creatorSettlementAccountRepository.findByUserId(userId);
    }

    @Override
    public List<CreatorSettlementAccount> getCreatorSettlementAccountsByUserIdAndStatus(Long userId, String status) {
        return creatorSettlementAccountRepository.findByUserIdAndStatus(userId, status);
    }
}
