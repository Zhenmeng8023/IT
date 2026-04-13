package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {

    Page<AiCallLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AiCallLog> findBySession_IdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    Optional<AiCallLog> findTopByTraceIdOrderByCreatedAtDesc(String traceId);

    Optional<AiCallLog> findTopByRequestIdOrderByCreatedAtDesc(String requestId);

    Optional<AiCallLog> findTopByMessage_IdOrderByCreatedAtDesc(Long messageId);
}
