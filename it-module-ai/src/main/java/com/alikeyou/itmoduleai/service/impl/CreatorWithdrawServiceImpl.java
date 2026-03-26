package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.WithdrawAccountCreateRequest;
import com.alikeyou.itmoduleai.dto.request.WithdrawItemRequest;
import com.alikeyou.itmoduleai.dto.request.WithdrawRequestCreateRequest;
import com.alikeyou.itmoduleai.entity.CreatorSettlementAccount;
import com.alikeyou.itmoduleai.entity.CreatorWithdrawItem;
import com.alikeyou.itmoduleai.entity.CreatorWithdrawRequest;
import com.alikeyou.itmoduleai.repository.CreatorSettlementAccountRepository;
import com.alikeyou.itmoduleai.repository.CreatorWithdrawItemRepository;
import com.alikeyou.itmoduleai.repository.CreatorWithdrawRequestRepository;
import com.alikeyou.itmoduleai.service.CreatorWithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CreatorWithdrawServiceImpl implements CreatorWithdrawService {

    private final CreatorSettlementAccountRepository creatorSettlementAccountRepository;
    private final CreatorWithdrawRequestRepository creatorWithdrawRequestRepository;
    private final CreatorWithdrawItemRepository creatorWithdrawItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CreatorSettlementAccount> listAccounts(Long creatorId) {
        return creatorSettlementAccountRepository.findByCreatorIdOrderByIsDefaultDescIdDesc(creatorId);
    }

    @Override
    public CreatorSettlementAccount saveAccount(WithdrawAccountCreateRequest request) {
        CreatorSettlementAccount entity = new CreatorSettlementAccount();
        entity.setCreatorId(request.getCreatorId());
        entity.setAccountType(request.getAccountType());
        entity.setAccountName(request.getAccountName());
        entity.setAccountNoMasked(request.getAccountNoMasked());
        entity.setAccountNoEncrypted(request.getAccountNoEncrypted());
        entity.setBankName(request.getBankName());
        entity.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        entity.setStatus(CreatorSettlementAccount.Status.ACTIVE);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return creatorSettlementAccountRepository.save(entity);
    }

    @Override
    public CreatorWithdrawRequest submit(WithdrawRequestCreateRequest request) {
        CreatorWithdrawRequest entity = new CreatorWithdrawRequest();
        entity.setCreatorId(request.getCreatorId());
        entity.setSettlementAccount(creatorSettlementAccountRepository.findById(request.getSettlementAccountId())
                .orElseThrow(() -> new RuntimeException("结算账户不存在")));
        entity.setRequestNo(generateRequestNo());
        entity.setAmount(request.getAmount());
        entity.setStatus(CreatorWithdrawRequest.Status.PENDING);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        CreatorWithdrawRequest saved = creatorWithdrawRequestRepository.save(entity);

        List<CreatorWithdrawItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (WithdrawItemRequest itemRequest : request.getItems()) {
                if (creatorWithdrawItemRepository.existsByRevenueRecordId(itemRequest.getRevenueRecordId())) {
                    throw new RuntimeException("收益记录已被提现使用");
                }
                CreatorWithdrawItem item = new CreatorWithdrawItem();
                item.setWithdrawRequest(saved);
                item.setRevenueRecordId(itemRequest.getRevenueRecordId());
                item.setAmount(itemRequest.getAmount());
                item.setCreatedAt(Instant.now());
                items.add(item);
            }
            creatorWithdrawItemRepository.saveAll(items);
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CreatorWithdrawRequest> pageRequests(Long creatorId, Pageable pageable) {
        return creatorWithdrawRequestRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreatorWithdrawItem> listItems(Long requestId) {
        return creatorWithdrawItemRepository.findByWithdrawRequest_IdOrderByIdAsc(requestId);
    }

    @Override
    public CreatorWithdrawRequest approve(Long requestId, String auditRemark) {
        CreatorWithdrawRequest entity = getRequest(requestId);
        entity.setStatus(CreatorWithdrawRequest.Status.APPROVED);
        entity.setAuditRemark(auditRemark);
        entity.setUpdatedAt(Instant.now());
        return creatorWithdrawRequestRepository.save(entity);
    }

    @Override
    public CreatorWithdrawRequest reject(Long requestId, String auditRemark) {
        CreatorWithdrawRequest entity = getRequest(requestId);
        entity.setStatus(CreatorWithdrawRequest.Status.REJECTED);
        entity.setAuditRemark(auditRemark);
        entity.setUpdatedAt(Instant.now());
        return creatorWithdrawRequestRepository.save(entity);
    }

    @Override
    public CreatorWithdrawRequest markPaid(Long requestId) {
        CreatorWithdrawRequest entity = getRequest(requestId);
        entity.setStatus(CreatorWithdrawRequest.Status.PAID);
        entity.setPaidAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return creatorWithdrawRequestRepository.save(entity);
    }

    private CreatorWithdrawRequest getRequest(Long requestId) {
        return creatorWithdrawRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("提现申请不存在"));
    }

    private String generateRequestNo() {
        String requestNo = "WD" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        while (creatorWithdrawRequestRepository.existsByRequestNo(requestNo)) {
            requestNo = "WD" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        }
        return requestNo;
    }
}
