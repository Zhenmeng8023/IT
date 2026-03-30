package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeImportTaskRepository extends JpaRepository<KnowledgeImportTask, Long> {

    List<KnowledgeImportTask> findByKnowledgeBase_IdOrderByCreatedAtDesc(Long knowledgeBaseId);
}
