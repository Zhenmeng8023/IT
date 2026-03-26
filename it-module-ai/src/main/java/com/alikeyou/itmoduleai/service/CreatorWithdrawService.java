package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.dto.request.WithdrawAccountCreateRequest;
import com.alikeyou.itmoduleai.dto.request.WithdrawRequestCreateRequest;
import com.alikeyou.itmoduleai.entity.CreatorSettlementAccount;
import com.alikeyou.itmoduleai.entity.CreatorWithdrawItem;
import com.alikeyou.itmoduleai.entity.CreatorWithdrawRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CreatorWithdrawService {

    List<CreatorSettlementAccount> listAccounts(Long creatorId);

    CreatorSettlementAccount saveAccount(WithdrawAccountCreateRequest request);

    CreatorWithdrawRequest submit(WithdrawRequestCreateRequest request);

    Page<CreatorWithdrawRequest> pageRequests(Long creatorId, Pageable pageable);

    List<CreatorWithdrawItem> listItems(Long requestId);

    CreatorWithdrawRequest approve(Long requestId, String auditRemark);

    CreatorWithdrawRequest reject(Long requestId, String auditRemark);

    CreatorWithdrawRequest markPaid(Long requestId);
}
