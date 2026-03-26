package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiSessionKnowledgeBaseRepository extends JpaRepository<AiSessionKnowledgeBase, Long> {

    List<AiSessionKnowledgeBase> findBySession_IdOrderByPriorityAscIdAsc(Long sessionId);

    void deleteBySession_Id(Long sessionId);
}
