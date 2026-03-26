package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.WithdrawAccountCreateRequest;
import com.alikeyou.itmoduleai.dto.request.WithdrawRequestCreateRequest;
import com.alikeyou.itmoduleai.entity.CreatorSettlementAccount;
import com.alikeyou.itmoduleai.entity.CreatorWithdrawItem;
import com.alikeyou.itmoduleai.entity.CreatorWithdrawRequest;
import com.alikeyou.itmoduleai.service.CreatorWithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/withdraw")
@RequiredArgsConstructor
public class CreatorWithdrawController {

    private final CreatorWithdrawService creatorWithdrawService;

    @GetMapping("/accounts/{creatorId}")
    public ApiResponse<List<CreatorSettlementAccount>> listAccounts(@PathVariable Long creatorId) {
        return ApiResponse.ok(creatorWithdrawService.listAccounts(creatorId));
    }

    @PostMapping("/accounts")
    public ApiResponse<CreatorSettlementAccount> saveAccount(@RequestBody WithdrawAccountCreateRequest request) {
        return ApiResponse.ok("保存成功", creatorWithdrawService.saveAccount(request));
    }

    @PostMapping("/requests")
    public ApiResponse<CreatorWithdrawRequest> submit(@RequestBody WithdrawRequestCreateRequest request) {
        return ApiResponse.ok("申请成功", creatorWithdrawService.submit(request));
    }

    @GetMapping("/requests/{creatorId}")
    public ApiResponse<Page<CreatorWithdrawRequest>> pageRequests(@PathVariable Long creatorId, Pageable pageable) {
        return ApiResponse.ok(creatorWithdrawService.pageRequests(creatorId, pageable));
    }

    @GetMapping("/request/{requestId}/items")
    public ApiResponse<List<CreatorWithdrawItem>> listItems(@PathVariable Long requestId) {
        return ApiResponse.ok(creatorWithdrawService.listItems(requestId));
    }

    @PutMapping("/request/{requestId}/approve")
    public ApiResponse<CreatorWithdrawRequest> approve(@PathVariable Long requestId,
                                                       @RequestParam(required = false) String auditRemark) {
        return ApiResponse.ok("审核通过", creatorWithdrawService.approve(requestId, auditRemark));
    }

    @PutMapping("/request/{requestId}/reject")
    public ApiResponse<CreatorWithdrawRequest> reject(@PathVariable Long requestId,
                                                      @RequestParam(required = false) String auditRemark) {
        return ApiResponse.ok("已拒绝", creatorWithdrawService.reject(requestId, auditRemark));
    }

    @PutMapping("/request/{requestId}/paid")
    public ApiResponse<CreatorWithdrawRequest> markPaid(@PathVariable Long requestId) {
        return ApiResponse.ok("已打款", creatorWithdrawService.markPaid(requestId));
    }
}
