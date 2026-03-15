package com.alikeyou.itmodulesystem.repository;

import com.alikeyou.itmodulesystem.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 根据操作类型查询审计日志
     */
    List<AuditLog> findByAction(String action);

    /**
     * 根据目标类型和目标 ID 查询审计日志
     */
    List<AuditLog> findByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 根据操作时间范围查询审计日志
     */
    List<AuditLog> findByCreatedAtBetween(Instant startTime, Instant endTime);

    /**
     * 根据目标类型查询审计日志
     */
    List<AuditLog> findByTargetType(String targetType);

    /**
     * 搜索审计日志（支持操作类型、目标类型等）
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:targetType IS NULL OR a.targetType = :targetType) AND " +
            "(:startTime IS NULL OR a.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR a.createdAt <= :endTime)")
    List<AuditLog> searchAuditLogs(
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * 统计某个操作类型的数量
     */
    long countByAction(String action);

    /**
     * 统计某个目标类型的操作数量
     */
    long countByTargetType(String targetType);
}
