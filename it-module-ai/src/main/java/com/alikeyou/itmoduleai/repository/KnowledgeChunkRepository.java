package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByDocument_IdOrderByChunkIndexAsc(Long documentId);

    List<KnowledgeChunk> findByKnowledgeBase_IdOrderByDocument_IdAscChunkIndexAsc(Long knowledgeBaseId);

    long countByKnowledgeBase_Id(Long knowledgeBaseId);

    long countByDocument_Id(Long documentId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from KnowledgeChunk kc
            where kc.document.id = :documentId
            """)
    void deleteByDocumentId(@Param("documentId") Long documentId);

    @Query("""
            select distinct kc
            from KnowledgeChunk kc
            join fetch kc.knowledgeBase kb
            join fetch kc.document d
            where kb.id in :knowledgeBaseIds
            order by kc.createdAt desc, kc.id desc
            """)
    List<KnowledgeChunk> findRecentCandidatesByKnowledgeBaseIds(
            @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
            Pageable pageable
    );

    @Query("""
            select distinct kc
            from KnowledgeChunk kc
            join fetch kc.knowledgeBase kb
            join fetch kc.document d
            where kb.id in :knowledgeBaseIds
            order by d.id asc, kc.chunkIndex asc, kc.id asc
            """)
    List<KnowledgeChunk> findDocumentOrderedCandidatesByKnowledgeBaseIds(
            @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
            Pageable pageable
    );

    @Query("""
            select kc
            from KnowledgeChunk kc
            join fetch kc.knowledgeBase kb
            join fetch kc.document d
            where kc.id in :ids
            """)
    List<KnowledgeChunk> findAllByIdWithRefs(@Param("ids") Collection<Long> ids);
}
