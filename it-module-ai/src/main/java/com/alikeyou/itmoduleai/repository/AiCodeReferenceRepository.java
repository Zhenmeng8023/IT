package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiCodeReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface AiCodeReferenceRepository extends JpaRepository<AiCodeReference, Long> {

    List<AiCodeReference> findByFromSymbol_IdOrderByCreatedAtAsc(Long fromSymbolId);

    List<AiCodeReference> findByToSymbol_IdOrderByCreatedAtAsc(Long toSymbolId);

    List<AiCodeReference> findByFromDocument_IdOrderByCreatedAtAsc(Long fromDocumentId);

    Optional<AiCodeReference> findByReferenceKey(String referenceKey);

    @Query("""
            select distinct r
            from AiCodeReference r
            join fetch r.knowledgeBase kb
            left join fetch r.fromSymbol fs
            left join fetch r.toSymbol ts
            join fetch r.fromDocument fd
            left join fetch r.fromChunk fc
            left join fetch r.toDocument td
            left join fetch r.toChunk tc
            where kb.id in :knowledgeBaseIds
              and r.status = com.alikeyou.itmoduleai.entity.AiCodeReference.Status.ACTIVE
              and r.resolutionStatus = com.alikeyou.itmoduleai.entity.AiCodeReference.ResolutionStatus.RESOLVED
              and (
                    fs.id in :symbolIds
                    or ts.id in :symbolIds
              )
            order by r.createdAt asc, r.id asc
            """)
    List<AiCodeReference> findResolvedGraphEdgesBySymbolIds(
            @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
            @Param("symbolIds") Collection<Long> symbolIds
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AiCodeReference r
            where r.fromDocument.id = :documentId
            """)
    void deleteByFromDocumentId(@Param("documentId") Long documentId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AiCodeReference r
            where r.fromDocument.id = :documentId
               or r.toDocument.id = :documentId
               or r.fromChunk.id in (
                    select c.id
                    from KnowledgeChunk c
                    where c.document.id = :documentId
               )
               or r.toChunk.id in (
                    select c.id
                    from KnowledgeChunk c
                    where c.document.id = :documentId
               )
               or r.fromSymbol.id in (
                    select s.id
                    from AiCodeSymbol s
                    where s.document.id = :documentId
               )
               or r.toSymbol.id in (
                    select s.id
                    from AiCodeSymbol s
                    where s.document.id = :documentId
               )
            """)
    void deleteByDocumentGraphId(@Param("documentId") Long documentId);
}
