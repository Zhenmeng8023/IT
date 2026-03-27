package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeIndexTaskRepository extends JpaRepository<KnowledgeIndexTask, Long> {

    List<KnowledgeIndexTask> findByStatusOrderByCreatedAtAsc(KnowledgeIndexTask.Status status);

    List<KnowledgeIndexTask> findByDocument_IdOrderByCreatedAtDesc(Long documentId);

    List<KnowledgeIndexTask> findByKnowledgeBase_IdOrderByCreatedAtDesc(Long knowledgeBaseId);
}