package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    @Query("SELECT kc FROM KnowledgeChunk kc WHERE kc.document.id = :docId")
    List<KnowledgeChunk> findByDocumentId(@Param("docId") Long documentId);

    @Query("SELECT kc FROM KnowledgeChunk kc WHERE kc.knowledgeBase.id = :kbId AND kc.status = :status")
    List<KnowledgeChunk> findByKnowledgeBaseIdAndStatus(@Param("kbId") Long knowledgeBaseId,
                                                        KnowledgeChunk.Status status);

    @Query("SELECT kc FROM KnowledgeChunk kc WHERE kc.document.id = :docId AND kc.status = 'ACTIVE' ORDER BY kc.chunkIndex ASC")
    List<KnowledgeChunk> findActiveChunksByDocument(@Param("docId") Long docId);
}
