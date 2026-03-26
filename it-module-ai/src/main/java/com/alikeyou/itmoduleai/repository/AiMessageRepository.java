package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    Page<AiMessage> findBySession_IdOrderByCreatedAtAsc(Long sessionId, Pageable pageable);

    Page<AiMessage> findBySession_IdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
}
