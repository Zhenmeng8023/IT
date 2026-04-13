package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiSessionKnowledgeBaseRepository extends JpaRepository<AiSessionKnowledgeBase, Long> {

    List<AiSessionKnowledgeBase> findBySession_IdOrderByPriorityAscIdAsc(Long sessionId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from AiSessionKnowledgeBase binding
            where binding.session.id = :sessionId
            """)
    void deleteBySession_Id(@Param("sessionId") Long sessionId);
}
