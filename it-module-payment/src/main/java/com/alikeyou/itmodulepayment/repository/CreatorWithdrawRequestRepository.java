package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.CreatorWithdrawRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreatorWithdrawRequestRepository extends JpaRepository<CreatorWithdrawRequest, Long> {

    // 根据用户ID查询提现请求
    List<CreatorWithdrawRequest> findByUserId(Long userId);

    // 根据状态查询提现请求
    List<CreatorWithdrawRequest> findByStatus(String status);

    // 根据用户ID和状态查询提现请求
    List<CreatorWithdrawRequest> findByUserIdAndStatus(Long userId, String status);
}