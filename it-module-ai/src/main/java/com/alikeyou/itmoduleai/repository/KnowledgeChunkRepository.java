package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByDocument_IdOrderByChunkIndexAsc(Long documentId);

    long countByKnowledgeBase_Id(Long knowledgeBaseId);

    void deleteByDocument_Id(Long documentId);

    @Query("""
            select kc
            from KnowledgeChunk kc
            join fetch kc.knowledgeBase kb
            join fetch kc.document d
            where kb.id in :knowledgeBaseIds
            order by kc.createdAt desc, kc.id desc
            """)
    List<KnowledgeChunk> findRecentCandidatesByKnowledgeBaseIds(@Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
                                                                Pageable pageable);
}