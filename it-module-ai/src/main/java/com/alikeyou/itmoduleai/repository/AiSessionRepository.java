package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

@Repository
public interface AiSessionRepository extends JpaRepository<AiSession, Long> {

    @Query("""
            select s
            from AiSession s
            where s.userId = :userId
              and (:bizType is null or s.bizType = :bizType)
              and (:status is null or s.status = :status)
              and (
                    :knowledgeBaseId is null
                    or (s.defaultKnowledgeBase is not null and s.defaultKnowledgeBase.id = :knowledgeBaseId)
                    or exists (
                        select 1
                        from AiSessionKnowledgeBase sk
                        where sk.session = s
                          and sk.knowledgeBase.id = :knowledgeBaseId
                    )
                  )
            order by s.updatedAt desc
            """)
    Page<AiSession> searchUserSessions(@Param("userId") Long userId,
                                       @Param("bizType") AiSession.BizType bizType,
                                       @Param("knowledgeBaseId") Long knowledgeBaseId,
                                       @Param("status") AiSession.Status status,
                                       Pageable pageable);
}