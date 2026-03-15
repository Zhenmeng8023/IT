package com.alikeyou.itmodulesystem.service;

import com.alikeyou.itmodulesystem.entity.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuditLogService {

    /**
     * 记录审计日志
     */
    AuditLog logAction(String action, String targetType, Long targetId,
                       String ipAddress, String userAgent, Map<String, Object> details);

    /**
     * 保存审计日志
     */
    AuditLog saveAuditLog(AuditLog auditLog);

    /**
     * 根据 ID 获取审计日志
     */
    Optional<AuditLog> getAuditLogById(Long id);

    /**
     * 获取所有审计日志
     */
    List<AuditLog> getAllAuditLogs();

    /**
     * 根据操作类型查询审计日志
     */
    List<AuditLog> getAuditLogsByAction(String action);

    /**
     * 根据目标类型和目标 ID 查询审计日志
     */
    List<AuditLog> getAuditLogsByTarget(String targetType, Long targetId);

    /**
     * 根据时间范围查询审计日志
     */
    List<AuditLog> getAuditLogsByTimeRange(Instant startTime, Instant endTime);

    /**
     * 搜索审计日志
     */
    List<AuditLog> searchAuditLogs(String action, String targetType,
                                   Instant startTime, Instant endTime);

    /**
     * 删除审计日志
     */
    void deleteAuditLog(Long id);

    /**
     * 统计操作数量
     */
    long countByAction(String action);

    /**
     * 统计目标类型操作数量
     */
    long countByTargetType(String targetType);
}
