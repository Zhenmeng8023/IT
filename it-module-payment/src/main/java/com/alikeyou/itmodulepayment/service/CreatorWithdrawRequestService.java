package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.CreatorWithdrawRequestDTO;
import com.alikeyou.itmodulepayment.entity.CreatorWithdrawRequest;

import java.math.BigDecimal;
import java.util.List;

public interface CreatorWithdrawRequestService {

    /**
     * 提交提现申请（核心业务方法）
     * @param userId 用户ID
     * @param settlementAccountId 结算账户ID
     * @param withdrawAmount 提现金额
     * @param remark 备注
     * @return 提现请求实体
     */
    CreatorWithdrawRequest submitWithdrawRequest(Long userId, Long settlementAccountId, 
                                                  BigDecimal withdrawAmount, String remark);

    /**
     * 审核通过提现申请
     * @param id 提现申请ID
     * @param reviewedBy 审核人ID
     * @param reviewNote 审核备注
     * @return 更新后的提现请求
     */
    CreatorWithdrawRequest approveWithdrawRequest(Long id, Long reviewedBy, String reviewNote);

    /**
     * 拒绝提现申请
     * @param id 提现申请ID
     * @param reviewedBy 审核人ID
     * @param reviewNote 拒绝原因
     * @return 更新后的提现请求
     */
    CreatorWithdrawRequest rejectWithdrawRequest(Long id, Long reviewedBy, String reviewNote);

    /**
     * 执行打款（调用支付宝转账API）
     * @param id 提现申请ID
     * @return 更新后的提现请求
     */
    CreatorWithdrawRequest processWithdrawPayment(Long id);

    /**
     * 获取用户可提现余额
     * @param userId 用户ID
     * @return 可提现金额
     */
    BigDecimal getAvailableWithdrawBalance(Long userId);

    // ========== 以下为原有的查询方法 ==========

    // 根据ID查询创作者提现请求
    CreatorWithdrawRequest getCreatorWithdrawRequestById(Long id);

    // 根据用户ID查询创作者提现请求
    List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByUserId(Long userId);

    // 根据状态查询创作者提现请求
    List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByStatus(String status);

    // 根据用户ID和状态查询创作者提现请求
    List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByUserIdAndStatus(Long userId, String status);
}