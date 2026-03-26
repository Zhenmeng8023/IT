package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    Page<KnowledgeDocument> findByKnowledgeBase_IdOrderByUpdatedAtDesc(Long knowledgeBaseId, Pageable pageable);

    long countByKnowledgeBase_Id(Long knowledgeBaseId);
}
