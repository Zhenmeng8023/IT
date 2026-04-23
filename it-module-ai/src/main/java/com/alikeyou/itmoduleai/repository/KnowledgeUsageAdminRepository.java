package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageQuotaConfigDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserQueryDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class KnowledgeUsageAdminRepository {

    private static final int DEFAULT_MAX_KNOWLEDGE_BASE_COUNT = 20;
    private static final int DEFAULT_MAX_DOCUMENT_COUNT = 500;
    private static final int DEFAULT_MAX_DAILY_QUESTION_COUNT = 200;
    private static final int DEFAULT_MAX_DAILY_IMPORT_COUNT = 50;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Page<UserPolicyRow> pageUsers(KnowledgeUsageUserQueryDTO query, Pageable pageable) {
        MapSqlParameterSource params = buildUserFilterParams(query);
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        String baseSql = """
                FROM user_info u
                LEFT JOIN ai_user_knowledge_usage_policy p ON p.user_id = u.id
                WHERE (:keyword IS NULL OR :keyword = ''
                       OR LOWER(u.username) LIKE CONCAT('%', LOWER(:keyword), '%')
                       OR LOWER(COALESCE(u.nickname, '')) LIKE CONCAT('%', LOWER(:keyword), '%')
                       OR LOWER(COALESCE(u.email, '')) LIKE CONCAT('%', LOWER(:keyword), '%'))
                  AND (:roleId IS NULL OR u.role_id = :roleId)
                  AND (:userStatus IS NULL OR :userStatus = '' OR u.status = :userStatus)
                  AND (:frozenFlag IS NULL OR COALESCE(p.frozen, 0) = :frozenFlag)
                  AND (:importEnabledFlag IS NULL OR COALESCE(p.import_enabled, 1) = :importEnabledFlag)
                  AND (:qaEnabledFlag IS NULL OR COALESCE(p.qa_enabled, 1) = :qaEnabledFlag)
                """;

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) " + baseSql,
                params,
                Long.class
        );

        List<UserPolicyRow> content = jdbcTemplate.query("""
                        SELECT u.id,
                               u.username,
                               u.nickname,
                               u.email,
                               u.status,
                               u.role_id,
                               u.last_login_at,
                               u.last_active_at,
                               COALESCE(p.frozen, 0) AS frozen,
                               COALESCE(p.import_enabled, 1) AS import_enabled,
                               COALESCE(p.qa_enabled, 1) AS qa_enabled,
                               COALESCE(p.max_knowledge_base_count, :defaultMaxKnowledgeBaseCount) AS max_knowledge_base_count,
                               COALESCE(p.max_document_count, :defaultMaxDocumentCount) AS max_document_count,
                               COALESCE(p.max_daily_question_count, :defaultMaxDailyQuestionCount) AS max_daily_question_count,
                               COALESCE(p.max_daily_import_count, :defaultMaxDailyImportCount) AS max_daily_import_count,
                               p.remark,
                               p.updated_at AS policy_updated_at,
                               p.updated_by AS policy_updated_by
                        """ + baseSql + """
                        ORDER BY u.id DESC
                        LIMIT :limit OFFSET :offset
                        """,
                withDefaultQuotaParams(params),
                userPolicyRowMapper()
        );

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    public Map<Long, KnowledgeAggregateRow> queryKnowledgeAggregates(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<KnowledgeAggregateRow> rows = jdbcTemplate.query("""
                        SELECT kb.owner_id AS user_id,
                               COUNT(kb.id) AS knowledge_base_count,
                               SUM(CASE WHEN kb.scope_type = 'PERSONAL' THEN 1 ELSE 0 END) AS personal_knowledge_base_count,
                               COALESCE(SUM(kb.doc_count), 0) AS document_count,
                               COALESCE(SUM(kb.chunk_count), 0) AS chunk_count,
                               COALESCE(MAX(kb.doc_count), 0) AS max_document_count_per_knowledge_base,
                               MAX(kb.updated_at) AS last_knowledge_update_at
                        FROM knowledge_base kb
                        WHERE kb.owner_id IN (:userIds)
                        GROUP BY kb.owner_id
                        """,
                new MapSqlParameterSource("userIds", userIds),
                knowledgeAggregateRowMapper()
        );
        return rowsToMap(rows, KnowledgeAggregateRow::getUserId);
    }

    public Map<Long, MemberAggregateRow> queryMemberAggregates(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MemberAggregateRow> rows = jdbcTemplate.query("""
                        SELECT m.user_id,
                               COUNT(DISTINCT m.knowledge_base_id) AS member_knowledge_base_count,
                               COALESCE(SUM(kb.doc_count), 0) AS project_document_count
                        FROM knowledge_base_member m
                        JOIN knowledge_base kb ON kb.id = m.knowledge_base_id
                        WHERE m.user_id IN (:userIds)
                          AND kb.scope_type = 'PROJECT'
                          AND kb.owner_id <> m.user_id
                        GROUP BY m.user_id
                        """,
                new MapSqlParameterSource("userIds", userIds),
                memberAggregateRowMapper()
        );
        return rowsToMap(rows, MemberAggregateRow::getUserId);
    }

    public Map<Long, CallAggregateRow> queryCallAggregates(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CallAggregateRow> rows = jdbcTemplate.query("""
                        SELECT c.user_id,
                               COUNT(1) AS qa_call_count,
                               SUM(CASE WHEN c.created_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY) THEN 1 ELSE 0 END) AS qa_count_last_7_days,
                               SUM(CASE WHEN c.created_at >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') THEN 1 ELSE 0 END) AS qa_count_this_month,
                               COALESCE(SUM(c.total_tokens), 0) AS total_tokens,
                               COALESCE(SUM(CASE WHEN c.created_at >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
                                                 THEN c.total_tokens ELSE 0 END), 0) AS token_count_this_month,
                               MAX(c.created_at) AS last_qa_at
                        FROM ai_call_log c
                        WHERE c.user_id IN (:userIds)
                          AND c.knowledge_base_id IS NOT NULL
                        GROUP BY c.user_id
                        """,
                new MapSqlParameterSource("userIds", userIds),
                callAggregateRowMapper()
        );
        return rowsToMap(rows, CallAggregateRow::getUserId);
    }

    public Map<Long, ImportAggregateRow> queryImportAggregates(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ImportAggregateRow> rows = jdbcTemplate.query("""
                        SELECT t.created_by AS user_id,
                               COUNT(1) AS import_count,
                               SUM(CASE WHEN t.created_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY) THEN 1 ELSE 0 END) AS import_count_last_7_days,
                               SUM(CASE WHEN t.created_at >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') THEN 1 ELSE 0 END) AS import_count_this_month,
                               MAX(t.created_at) AS last_import_at
                        FROM knowledge_import_task t
                        WHERE t.created_by IN (:userIds)
                        GROUP BY t.created_by
                        """,
                new MapSqlParameterSource("userIds", userIds),
                importAggregateRowMapper()
        );
        return rowsToMap(rows, ImportAggregateRow::getUserId);
    }

    public PolicyRow getPolicyOrDefault(Long userId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT user_id,
                                   frozen,
                                   import_enabled,
                                   qa_enabled,
                                   max_knowledge_base_count,
                                   max_document_count,
                                   max_daily_question_count,
                                   max_daily_import_count,
                                   remark,
                                   updated_at,
                                   updated_by
                            FROM ai_user_knowledge_usage_policy
                            WHERE user_id = :userId
                            """,
                    new MapSqlParameterSource("userId", userId),
                    policyRowMapper()
            );
        } catch (EmptyResultDataAccessException ex) {
            return defaultPolicy(userId);
        }
    }

    public PolicyRow updateQuota(Long userId, KnowledgeUsageQuotaConfigDTO quotaConfig, Long operatorId) {
        ensurePolicyRow(userId, operatorId);
        Integer resolvedDailyQaLimit = resolveDailyQuestionLimit(quotaConfig);
        Integer resolvedDailyImportLimit = resolveDailyImportLimit(quotaConfig);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("maxKnowledgeBaseCount", normalizeQuota(quotaConfig.getMaxKnowledgeBaseCount(), DEFAULT_MAX_KNOWLEDGE_BASE_COUNT))
                .addValue("maxDocumentCount", normalizeQuota(quotaConfig.getMaxDocumentCount(), DEFAULT_MAX_DOCUMENT_COUNT))
                .addValue("maxDailyQuestionCount", normalizeQuota(resolvedDailyQaLimit, DEFAULT_MAX_DAILY_QUESTION_COUNT))
                .addValue("maxDailyImportCount", normalizeQuota(resolvedDailyImportLimit, DEFAULT_MAX_DAILY_IMPORT_COUNT))
                .addValue("remark", trimToNull(quotaConfig.getRemark()))
                .addValue("operatorId", operatorId);

        jdbcTemplate.update("""
                        UPDATE ai_user_knowledge_usage_policy
                        SET max_knowledge_base_count = :maxKnowledgeBaseCount,
                            max_document_count = :maxDocumentCount,
                            max_daily_question_count = :maxDailyQuestionCount,
                            max_daily_import_count = :maxDailyImportCount,
                            remark = :remark,
                            updated_by = :operatorId,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = :userId
                        """,
                params
        );
        insertActionLog(userId, "UPDATE_QUOTA", trimToNull(quotaConfig.getRemark()), operatorId);
        return getPolicyOrDefault(userId);
    }

    public PolicyRow updateGovernance(Long userId,
                                      Boolean frozen,
                                      Boolean importEnabled,
                                      Boolean qaEnabled,
                                      Long operatorId,
                                      String actionCode,
                                      String reason) {
        ensurePolicyRow(userId, operatorId);
        StringBuilder sql = new StringBuilder("""
                UPDATE ai_user_knowledge_usage_policy
                SET updated_by = :operatorId,
                    updated_at = CURRENT_TIMESTAMP
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("operatorId", operatorId);

        if (frozen != null) {
            sql.append(", frozen = :frozen");
            params.addValue("frozen", frozen);
        }
        if (importEnabled != null) {
            sql.append(", import_enabled = :importEnabled");
            params.addValue("importEnabled", importEnabled);
        }
        if (qaEnabled != null) {
            sql.append(", qa_enabled = :qaEnabled");
            params.addValue("qaEnabled", qaEnabled);
        }
        sql.append(" WHERE user_id = :userId");

        jdbcTemplate.update(sql.toString(), params);
        insertActionLog(userId, actionCode, trimToNull(reason), operatorId);
        return getPolicyOrDefault(userId);
    }

    private void ensurePolicyRow(Long userId, Long operatorId) {
        jdbcTemplate.update("""
                        INSERT INTO ai_user_knowledge_usage_policy
                            (user_id, frozen, import_enabled, qa_enabled,
                             max_knowledge_base_count, max_document_count,
                             max_daily_question_count, max_daily_import_count,
                             remark, updated_by, created_at, updated_at)
                        VALUES
                            (:userId, 0, 1, 1,
                             :defaultMaxKnowledgeBaseCount, :defaultMaxDocumentCount,
                             :defaultMaxDailyQuestionCount, :defaultMaxDailyImportCount,
                             NULL, :operatorId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE user_id = user_id
                        """,
                withDefaultQuotaParams(new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("operatorId", operatorId))
        );
    }

    private void insertActionLog(Long userId, String actionCode, String reason, Long operatorId) {
        jdbcTemplate.update("""
                        INSERT INTO ai_user_knowledge_usage_action_log
                            (user_id, action_code, reason, operator_id, created_at)
                        VALUES
                            (:userId, :actionCode, :reason, :operatorId, CURRENT_TIMESTAMP)
                        """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("actionCode", actionCode)
                        .addValue("reason", reason)
                        .addValue("operatorId", operatorId)
        );
    }

    private MapSqlParameterSource buildUserFilterParams(KnowledgeUsageUserQueryDTO query) {
        KnowledgeUsageUserQueryDTO safeQuery = query == null ? new KnowledgeUsageUserQueryDTO() : query;
        return new MapSqlParameterSource()
                .addValue("keyword", trimToNull(safeQuery.getKeyword()))
                .addValue("roleId", safeQuery.getRoleId())
                .addValue("userStatus", trimToNull(safeQuery.getUserStatus()))
                .addValue("frozenFlag", toDbBoolean(safeQuery.getFrozen()))
                .addValue("importEnabledFlag", toDbBoolean(safeQuery.getImportEnabled()))
                .addValue("qaEnabledFlag", toDbBoolean(safeQuery.getQaEnabled()));
    }

    private MapSqlParameterSource withDefaultQuotaParams(MapSqlParameterSource params) {
        return params.addValue("defaultMaxKnowledgeBaseCount", DEFAULT_MAX_KNOWLEDGE_BASE_COUNT)
                .addValue("defaultMaxDocumentCount", DEFAULT_MAX_DOCUMENT_COUNT)
                .addValue("defaultMaxDailyQuestionCount", DEFAULT_MAX_DAILY_QUESTION_COUNT)
                .addValue("defaultMaxDailyImportCount", DEFAULT_MAX_DAILY_IMPORT_COUNT);
    }

    private Integer normalizeQuota(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private Integer resolveDailyQuestionLimit(KnowledgeUsageQuotaConfigDTO quotaConfig) {
        if (quotaConfig == null) {
            return null;
        }
        if (quotaConfig.getMaxDailyQuestionCount() != null) {
            return quotaConfig.getMaxDailyQuestionCount();
        }
        return toDailyFromMonthly(quotaConfig.getMaxMonthlyQaCount());
    }

    private Integer resolveDailyImportLimit(KnowledgeUsageQuotaConfigDTO quotaConfig) {
        if (quotaConfig == null) {
            return null;
        }
        if (quotaConfig.getMaxDailyImportCount() != null) {
            return quotaConfig.getMaxDailyImportCount();
        }
        return toDailyFromMonthly(quotaConfig.getMaxMonthlyImportCount());
    }

    private Integer toDailyFromMonthly(Integer monthlyLimit) {
        if (monthlyLimit == null || monthlyLimit <= 0) {
            return null;
        }
        int daily = (int) Math.ceil(monthlyLimit / 30.0d);
        return Math.max(daily, 1);
    }

    private Integer toDbBoolean(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private PolicyRow defaultPolicy(Long userId) {
        return PolicyRow.builder()
                .userId(userId)
                .frozen(false)
                .importEnabled(true)
                .qaEnabled(true)
                .maxKnowledgeBaseCount(DEFAULT_MAX_KNOWLEDGE_BASE_COUNT)
                .maxDocumentCount(DEFAULT_MAX_DOCUMENT_COUNT)
                .maxDailyQuestionCount(DEFAULT_MAX_DAILY_QUESTION_COUNT)
                .maxDailyImportCount(DEFAULT_MAX_DAILY_IMPORT_COUNT)
                .build();
    }

    private RowMapper<UserPolicyRow> userPolicyRowMapper() {
        return (rs, rowNum) -> UserPolicyRow.builder()
                .userId(rs.getLong("id"))
                .username(rs.getString("username"))
                .nickname(rs.getString("nickname"))
                .email(rs.getString("email"))
                .userStatus(rs.getString("status"))
                .roleId((Integer) rs.getObject("role_id"))
                .lastLoginAt(getInstant(rs, "last_login_at"))
                .lastActiveAt(getInstant(rs, "last_active_at"))
                .frozen(rs.getBoolean("frozen"))
                .importEnabled(rs.getBoolean("import_enabled"))
                .qaEnabled(rs.getBoolean("qa_enabled"))
                .maxKnowledgeBaseCount(rs.getInt("max_knowledge_base_count"))
                .maxDocumentCount(rs.getInt("max_document_count"))
                .maxDailyQuestionCount(rs.getInt("max_daily_question_count"))
                .maxDailyImportCount(rs.getInt("max_daily_import_count"))
                .remark(rs.getString("remark"))
                .policyUpdatedAt(getInstant(rs, "policy_updated_at"))
                .policyUpdatedBy(getLong(rs, "policy_updated_by"))
                .build();
    }

    private RowMapper<KnowledgeAggregateRow> knowledgeAggregateRowMapper() {
        return (rs, rowNum) -> KnowledgeAggregateRow.builder()
                .userId(rs.getLong("user_id"))
                .knowledgeBaseCount(rs.getInt("knowledge_base_count"))
                .personalKnowledgeBaseCount(rs.getInt("personal_knowledge_base_count"))
                .documentCount(rs.getInt("document_count"))
                .chunkCount(rs.getInt("chunk_count"))
                .maxDocumentCountPerKnowledgeBase(rs.getInt("max_document_count_per_knowledge_base"))
                .lastKnowledgeUpdateAt(getInstant(rs, "last_knowledge_update_at"))
                .build();
    }

    private RowMapper<MemberAggregateRow> memberAggregateRowMapper() {
        return (rs, rowNum) -> MemberAggregateRow.builder()
                .userId(rs.getLong("user_id"))
                .memberKnowledgeBaseCount(rs.getInt("member_knowledge_base_count"))
                .projectDocumentCount(rs.getInt("project_document_count"))
                .build();
    }

    private RowMapper<CallAggregateRow> callAggregateRowMapper() {
        return (rs, rowNum) -> CallAggregateRow.builder()
                .userId(rs.getLong("user_id"))
                .qaCallCount(rs.getLong("qa_call_count"))
                .qaCountLast7Days(rs.getLong("qa_count_last_7_days"))
                .qaCountThisMonth(rs.getLong("qa_count_this_month"))
                .totalTokens(rs.getLong("total_tokens"))
                .tokenCountThisMonth(rs.getLong("token_count_this_month"))
                .lastQaAt(getInstant(rs, "last_qa_at"))
                .build();
    }

    private RowMapper<ImportAggregateRow> importAggregateRowMapper() {
        return (rs, rowNum) -> ImportAggregateRow.builder()
                .userId(rs.getLong("user_id"))
                .importCount(rs.getLong("import_count"))
                .importCountLast7Days(rs.getLong("import_count_last_7_days"))
                .importCountThisMonth(rs.getLong("import_count_this_month"))
                .lastImportAt(getInstant(rs, "last_import_at"))
                .build();
    }

    private RowMapper<PolicyRow> policyRowMapper() {
        return (rs, rowNum) -> PolicyRow.builder()
                .userId(rs.getLong("user_id"))
                .frozen(rs.getBoolean("frozen"))
                .importEnabled(rs.getBoolean("import_enabled"))
                .qaEnabled(rs.getBoolean("qa_enabled"))
                .maxKnowledgeBaseCount(rs.getInt("max_knowledge_base_count"))
                .maxDocumentCount(rs.getInt("max_document_count"))
                .maxDailyQuestionCount(rs.getInt("max_daily_question_count"))
                .maxDailyImportCount(rs.getInt("max_daily_import_count"))
                .remark(rs.getString("remark"))
                .updatedAt(getInstant(rs, "updated_at"))
                .updatedBy(getLong(rs, "updated_by"))
                .build();
    }

    private Instant getInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(Objects.toString(value));
    }

    private <T> Map<Long, T> rowsToMap(List<T> rows, java.util.function.Function<T, Long> keyExtractor) {
        Map<Long, T> result = new LinkedHashMap<>();
        for (T row : rows) {
            result.put(keyExtractor.apply(row), row);
        }
        return result;
    }

    @Getter
    @Builder
    public static class UserPolicyRow {
        private Long userId;
        private String username;
        private String nickname;
        private String email;
        private String userStatus;
        private Integer roleId;
        private Instant lastLoginAt;
        private Instant lastActiveAt;
        private Boolean frozen;
        private Boolean importEnabled;
        private Boolean qaEnabled;
        private Integer maxKnowledgeBaseCount;
        private Integer maxDocumentCount;
        private Integer maxDailyQuestionCount;
        private Integer maxDailyImportCount;
        private String remark;
        private Instant policyUpdatedAt;
        private Long policyUpdatedBy;
    }

    @Getter
    @Builder
    public static class KnowledgeAggregateRow {
        private Long userId;
        private Integer knowledgeBaseCount;
        private Integer personalKnowledgeBaseCount;
        private Integer documentCount;
        private Integer chunkCount;
        private Integer maxDocumentCountPerKnowledgeBase;
        private Instant lastKnowledgeUpdateAt;
    }

    @Getter
    @Builder
    public static class MemberAggregateRow {
        private Long userId;
        private Integer memberKnowledgeBaseCount;
        private Integer projectDocumentCount;
    }

    @Getter
    @Builder
    public static class CallAggregateRow {
        private Long userId;
        private Long qaCallCount;
        private Long qaCountLast7Days;
        private Long qaCountThisMonth;
        private Long totalTokens;
        private Long tokenCountThisMonth;
        private Instant lastQaAt;
    }

    @Getter
    @Builder
    public static class ImportAggregateRow {
        private Long userId;
        private Long importCount;
        private Long importCountLast7Days;
        private Long importCountThisMonth;
        private Instant lastImportAt;
    }

    @Getter
    @Builder
    public static class PolicyRow {
        private Long userId;
        private Boolean frozen;
        private Boolean importEnabled;
        private Boolean qaEnabled;
        private Integer maxKnowledgeBaseCount;
        private Integer maxDocumentCount;
        private Integer maxDailyQuestionCount;
        private Integer maxDailyImportCount;
        private String remark;
        private Instant updatedAt;
        private Long updatedBy;
    }
}
