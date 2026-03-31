package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeChunkEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeChunkEmbeddingRepository extends JpaRepository<KnowledgeChunkEmbedding, Long> {

    List<KnowledgeChunkEmbedding> findByChunk_IdOrderByIdDesc(Long chunkId);

    Optional<KnowledgeChunkEmbedding> findTopByChunk_IdOrderByIdDesc(Long chunkId);

    Optional<KnowledgeChunkEmbedding> findFirstByChunk_IdAndProviderCodeAndModelName(
            Long chunkId,
            String providerCode,
            String modelName
    );

    @Query("""
            select e
            from KnowledgeChunkEmbedding e
            where e.id in (
                select max(x.id)
                from KnowledgeChunkEmbedding x
                where x.chunk.id in :chunkIds
                group by x.chunk.id
            )
            """)
    List<KnowledgeChunkEmbedding> findLatestByChunkIds(@Param("chunkIds") Collection<Long> chunkIds);

    @Query("""
            select count(distinct e.chunk.id)
            from KnowledgeChunkEmbedding e
            where e.chunk.knowledgeBase.id = :knowledgeBaseId
            """)
    long countDistinctChunkByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    @Query("""
            select count(distinct e.chunk.id)
            from KnowledgeChunkEmbedding e
            where e.chunk.document.id = :documentId
            """)
    long countDistinctChunkByDocumentId(@Param("documentId") Long documentId);
}
