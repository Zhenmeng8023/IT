package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.dto.AiCallLogDTO;
import com.alikeyou.itmoduleai.dto.AiRetrievalLogDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * AI 日志服务接口
 */
public interface AiLogService {

    /**
     * 记录 AI 调用日志
     * @param logDTO 调用日志信息
     * @return 保存后的日志 ID
     */
    Long logAiCall(AiCallLogDTO logDTO);

    /**
     * 记录 AI 检索日志
     * @param logDTO 检索日志信息
     * @return 保存后的日志 ID
     */
    Long logAiRetrieval(AiRetrievalLogDTO logDTO);

    /**
     * 查询 AI 调用日志
     * @param userId 用户 ID
     * @param pageable 分页参数
     * @return 调用日志列表
     */
    Page<AiCallLogDTO> getCallLogs(Long userId, Pageable pageable);

    /**
     * 查询 AI 检索日志
     * @param callLogId 调用日志 ID
     * @return 检索日志列表
     */
    java.util.List<AiRetrievalLogDTO> getRetrievalLogs(Long callLogId);

    /**
     * 统计用户 AI 使用情况
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 使用统计
     */
    AiUsageStats getUserStats(Long userId, java.time.Instant startDate, java.time.Instant endDate);

    /**
     * AI 使用统计
     */
    record AiUsageStats(
            long totalCalls,
            long successfulCalls,
            long totalTokens,
            double totalCost
    ) {}
}