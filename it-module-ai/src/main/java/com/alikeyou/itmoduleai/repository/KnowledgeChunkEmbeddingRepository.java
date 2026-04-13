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

    Optional<KnowledgeChunkEmbedding> findTopByChunk_IdAndProviderCodeAndModelNameOrderByIdDesc(
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
            select e
            from KnowledgeChunkEmbedding e
            join fetch e.chunk c
            join fetch c.knowledgeBase kb
            join fetch c.document d
            where e.id in (
                select max(x.id)
                from KnowledgeChunkEmbedding x
                where x.chunk.knowledgeBase.id in :knowledgeBaseIds
                group by x.chunk.id
            )
            """)
    List<KnowledgeChunkEmbedding> findLatestByKnowledgeBaseIds(@Param("knowledgeBaseIds") Collection<Long> knowledgeBaseIds);

    @Query("""
            select e
            from KnowledgeChunkEmbedding e
            join fetch e.chunk c
            join fetch c.knowledgeBase kb
            join fetch c.document d
            where e.id in (
                select max(x.id)
                from KnowledgeChunkEmbedding x
                where x.chunk.knowledgeBase.id in :knowledgeBaseIds
                  and (
                        lower(trim(x.providerCode)) in :providerCodes
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, ' %')
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, '(%')
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, ' -%')
                  )
                  and (
                        lower(trim(x.modelName)) in :modelNames
                        or lower(trim(x.modelName)) like concat(:modelCanonical, ' %')
                        or lower(trim(x.modelName)) like concat(:modelCanonical, '(%')
                        or lower(trim(x.modelName)) like concat(:modelCanonical, ' -%')
                  )
                  and x.status = :status
                group by x.chunk.id
            )
            """)
    List<KnowledgeChunkEmbedding> findLatestActiveByKnowledgeBaseIdsAndProviderAndModel(
            @Param("knowledgeBaseIds") Collection<Long> knowledgeBaseIds,
            @Param("providerCodes") Collection<String> providerCodes,
            @Param("providerCanonical") String providerCanonical,
            @Param("modelNames") Collection<String> modelNames,
            @Param("modelCanonical") String modelCanonical,
            @Param("status") KnowledgeChunkEmbedding.Status status
    );

    @Query("""
            select count(e)
            from KnowledgeChunkEmbedding e
            where e.id in (
                select max(x.id)
                from KnowledgeChunkEmbedding x
                where x.chunk.knowledgeBase.id in :knowledgeBaseIds
                group by x.chunk.id
            )
            """)
    long countLatestByKnowledgeBaseIds(@Param("knowledgeBaseIds") Collection<Long> knowledgeBaseIds);

    @Query("""
            select count(e)
            from KnowledgeChunkEmbedding e
            where e.id in (
                select max(x.id)
                from KnowledgeChunkEmbedding x
                where x.chunk.knowledgeBase.id in :knowledgeBaseIds
                  and x.status = :status
                group by x.chunk.id
            )
            """)
    long countLatestByKnowledgeBaseIdsAndStatus(
            @Param("knowledgeBaseIds") Collection<Long> knowledgeBaseIds,
            @Param("status") KnowledgeChunkEmbedding.Status status
    );

    @Query("""
            select count(e)
            from KnowledgeChunkEmbedding e
            where e.id in (
                select max(x.id)
                from KnowledgeChunkEmbedding x
                where x.chunk.knowledgeBase.id in :knowledgeBaseIds
                  and x.status = :status
                  and (
                        lower(trim(x.providerCode)) in :providerCodes
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, ' %')
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, '(%')
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, ' -%')
                  )
                group by x.chunk.id
            )
            """)
    long countLatestByKnowledgeBaseIdsAndProviderAndStatus(
            @Param("knowledgeBaseIds") Collection<Long> knowledgeBaseIds,
            @Param("providerCodes") Collection<String> providerCodes,
            @Param("providerCanonical") String providerCanonical,
            @Param("status") KnowledgeChunkEmbedding.Status status
    );

    @Query("""
            select count(e)
            from KnowledgeChunkEmbedding e
            where e.id in (
                select max(x.id)
                from KnowledgeChunkEmbedding x
                where x.chunk.knowledgeBase.id in :knowledgeBaseIds
                  and x.status = :status
                  and (
                        lower(trim(x.providerCode)) in :providerCodes
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, ' %')
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, '(%')
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, ' -%')
                  )
                  and (
                        lower(trim(x.modelName)) in :modelNames
                        or lower(trim(x.modelName)) like concat(:modelCanonical, ' %')
                        or lower(trim(x.modelName)) like concat(:modelCanonical, '(%')
                        or lower(trim(x.modelName)) like concat(:modelCanonical, ' -%')
                  )
                group by x.chunk.id
            )
            """)
    long countLatestByKnowledgeBaseIdsAndProviderAndModelAndStatus(
            @Param("knowledgeBaseIds") Collection<Long> knowledgeBaseIds,
            @Param("providerCodes") Collection<String> providerCodes,
            @Param("providerCanonical") String providerCanonical,
            @Param("modelNames") Collection<String> modelNames,
            @Param("modelCanonical") String modelCanonical,
            @Param("status") KnowledgeChunkEmbedding.Status status
    );

    @Query("""
            select count(e)
            from KnowledgeChunkEmbedding e
            where e.id in (
                select max(x.id)
                from KnowledgeChunkEmbedding x
                where x.chunk.knowledgeBase.id in :knowledgeBaseIds
                  and (
                        lower(trim(x.providerCode)) in :providerCodes
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, ' %')
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, '(%')
                        or lower(trim(x.providerCode)) like concat(:providerCanonical, ' -%')
                  )
                  and (
                        lower(trim(x.modelName)) in :modelNames
                        or lower(trim(x.modelName)) like concat(:modelCanonical, ' %')
                        or lower(trim(x.modelName)) like concat(:modelCanonical, '(%')
                        or lower(trim(x.modelName)) like concat(:modelCanonical, ' -%')
                  )
                group by x.chunk.id
            )
              and e.status <> :status
            """)
    long countLatestByKnowledgeBaseIdsAndProviderAndModelAndStatusNot(
            @Param("knowledgeBaseIds") Collection<Long> knowledgeBaseIds,
            @Param("providerCodes") Collection<String> providerCodes,
            @Param("providerCanonical") String providerCanonical,
            @Param("modelNames") Collection<String> modelNames,
            @Param("modelCanonical") String modelCanonical,
            @Param("status") KnowledgeChunkEmbedding.Status status
    );

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
