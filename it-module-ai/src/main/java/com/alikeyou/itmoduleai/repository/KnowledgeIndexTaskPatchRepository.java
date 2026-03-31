package com.alikeyou.itmoduleai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class KnowledgeIndexTaskPatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public Long findKnowledgeBaseIdByDocumentId(Long documentId) {
        if (documentId == null) {
            return null;
        }
        return jdbcTemplate.query(
                "select knowledge_base_id from knowledge_document where id = ? limit 1",
                rs -> rs.next() ? rs.getLong(1) : null,
                documentId
        );
    }

    public boolean existsRunningEmbeddingTaskByKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(
                """
                select count(1)
                from knowledge_index_task
                where knowledge_base_id = ?
                  and task_type = 'EMBEDDING'
                  and status in ('RUNNING', 'PROCESSING', 'PENDING', 'INDEXING')
                """,
                Long.class,
                knowledgeBaseId
        );
        return count != null && count > 0;
    }

    public Long insertEmbeddingTask(Long knowledgeBaseId, Long documentId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Instant now = Instant.now();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into knowledge_index_task
                    (knowledge_base_id, document_id, task_type, status, retry_count, error_message, started_at)
                    values (?, ?, 'EMBEDDING', 'RUNNING', 0, null, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, knowledgeBaseId);
            if (documentId == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, documentId);
            }
            ps.setTimestamp(3, Timestamp.from(now));
            return ps;
        }, keyHolder);
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    public void markSuccess(Long taskId) {
        if (taskId == null) {
            return;
        }
        jdbcTemplate.update(
                """
                update knowledge_index_task
                   set status = 'SUCCESS',
                       error_message = null,
                       finished_at = ?
                 where id = ?
                """,
                Timestamp.from(Instant.now()),
                taskId
        );
    }

    public void markFailed(Long taskId, String errorMessage) {
        if (taskId == null) {
            return;
        }
        String safeMessage = StringUtils.hasText(errorMessage) ? errorMessage.trim() : "向量回填失败";
        if (safeMessage.length() > 500) {
            safeMessage = safeMessage.substring(0, 500);
        }
        jdbcTemplate.update(
                """
                update knowledge_index_task
                   set status = 'FAILED',
                       error_message = ?,
                       finished_at = ?
                 where id = ?
                """,
                safeMessage,
                Timestamp.from(Instant.now()),
                taskId
        );
    }
}
