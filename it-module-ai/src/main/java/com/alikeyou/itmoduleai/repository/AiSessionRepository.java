package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiSessionRepository extends JpaRepository<AiSession, Long> {

    Page<AiSession> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Page<AiSession> findByUserIdAndBizTypeOrderByUpdatedAtDesc(Long userId, AiSession.BizType bizType, Pageable pageable);
}
