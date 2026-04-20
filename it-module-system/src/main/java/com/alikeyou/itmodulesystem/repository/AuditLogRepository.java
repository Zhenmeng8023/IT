package com.alikeyou.itmodulesystem.repository;

import com.alikeyou.itmodulesystem.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    String ADMIN_LOG_FILTER_SQL = """
            WHERE (:startTime IS NULL OR a.created_at >= :startTime)
              AND (:endTime IS NULL OR a.created_at <= :endTime)
              AND (
                    :typeFilter IS NULL
                    OR LOWER(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.details, '$.type')), '')) = :typeFilter
                    OR (:typeFilter = 'security' AND LOWER(a.action) REGEXP 'login|password|auth')
                    OR (:typeFilter = 'error' AND LOWER(a.action) REGEXP 'error|exception|fail')
                    OR (:typeFilter = 'user' AND LOWER(a.action) REGEXP 'create|update|delete')
                    OR (
                        :typeFilter = 'system'
                        AND NOT (LOWER(a.action) REGEXP 'login|password|auth|error|exception|fail|create|update|delete')
                    )
              )
              AND (
                    :operatorFilter IS NULL
                    OR LOWER(CONCAT_WS(' ',
                        COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.details, '$.operator')), ''),
                        COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.details, '$.username')), ''),
                        COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.details, '$.userName')), ''),
                        COALESCE(CAST(a.user_id AS CHAR), '')
                    )) LIKE CONCAT('%', :operatorFilter, '%')
              )
              AND (
                    :moduleFilter IS NULL
                    OR LOWER(CONCAT_WS(' ',
                        COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.details, '$.module')), ''),
                        COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.details, '$.moduleName')), ''),
                        COALESCE(a.target_type, '')
                    )) LIKE CONCAT('%', :moduleFilter, '%')
              )
            """;

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

    List<AuditLog> findByCreatedAtBefore(Instant cutoff);

    long deleteByCreatedAtBefore(Instant cutoff);

    @Query(
            value = "SELECT a.* FROM audit_log a " + ADMIN_LOG_FILTER_SQL + " ORDER BY a.created_at DESC, a.id DESC",
            countQuery = "SELECT COUNT(1) FROM audit_log a " + ADMIN_LOG_FILTER_SQL,
            nativeQuery = true
    )
    Page<AuditLog> searchAdminLogs(
            @Param("typeFilter") String typeFilter,
            @Param("operatorFilter") String operatorFilter,
            @Param("moduleFilter") String moduleFilter,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable
    );

    @Query(
            value = "SELECT a.* FROM audit_log a " + ADMIN_LOG_FILTER_SQL + " ORDER BY a.created_at DESC, a.id DESC",
            nativeQuery = true
    )
    List<AuditLog> searchAdminLogsForExport(
            @Param("typeFilter") String typeFilter,
            @Param("operatorFilter") String operatorFilter,
            @Param("moduleFilter") String moduleFilter,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}
