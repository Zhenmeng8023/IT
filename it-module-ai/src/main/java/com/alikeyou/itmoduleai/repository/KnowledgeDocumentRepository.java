package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.knowledgeBase.id = :kbId ORDER BY kd.createdAt DESC")
    Page<KnowledgeDocument> findByKnowledgeBaseIdOrderByCreatedAtDesc(@Param("kbId") Long knowledgeBaseId, Pageable pageable);

    List<KnowledgeDocument> findByStatus(KnowledgeDocument.Status status);

    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.knowledgeBase.id = :kbId AND kd.status = 'INDEXED'")
    List<KnowledgeDocument> findIndexedDocuments(@Param("kbId") Long kbId);

    @Query("SELECT COUNT(kd) FROM KnowledgeDocument kd WHERE kd.knowledgeBase.id = :kbId")
    long countByKnowledgeBaseId(@Param("kbId") Long knowledgeBaseId);
}
