package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.CreatorWithdrawRequestDTO;
import com.alikeyou.itmodulepayment.entity.CreatorWithdrawRequest;

import java.util.List;

public interface CreatorWithdrawRequestService {

    // 创建创作者提现请求
    CreatorWithdrawRequest createCreatorWithdrawRequest(CreatorWithdrawRequestDTO dto);

    // 更新创作者提现请求
    CreatorWithdrawRequest updateCreatorWithdrawRequest(Long id, CreatorWithdrawRequestDTO dto);

    // 删除创作者提现请求
    void deleteCreatorWithdrawRequest(Long id);

    // 根据ID查询创作者提现请求
    CreatorWithdrawRequest getCreatorWithdrawRequestById(Long id);

    // 根据用户ID查询创作者提现请求
    List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByUserId(Long userId);

    // 根据状态查询创作者提现请求
    List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByStatus(String status);

    // 根据用户ID和状态查询创作者提现请求
    List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByUserIdAndStatus(Long userId, String status);
}