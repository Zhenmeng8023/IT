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

    List<KnowledgeChunk> findByPrimarySymbol_IdOrderByStartLineAsc(Long primarySymbolId);

    @Query("""
            select kc
            from KnowledgeChunk kc
            join fetch kc.knowledgeBase kb
            join fetch kc.document d
            where d.id = :documentId
              and kc.chunkIndex between :fromIndex and :toIndex
            order by kc.chunkIndex asc, kc.id asc
            """)
    List<KnowledgeChunk> findAdjacentCandidates(
            @Param("documentId") Long documentId,
            @Param("fromIndex") Integer fromIndex,
            @Param("toIndex") Integer toIndex
    );

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
        select distinct kc
        from KnowledgeChunk kc
        join fetch kc.knowledgeBase kb
        join fetch kc.document d
        where kb.id in :knowledgeBaseIds
          and (
                coalesce(kc.content, '') like concat('%', :term0, '%')
                or lower(coalesce(d.title, '')) like concat('%', :term0, '%')
                or lower(coalesce(d.fileName, '')) like concat('%', :term0, '%')
                or lower(coalesce(d.archiveEntryPath, '')) like concat('%', :term0, '%')
                or lower(coalesce(d.sourceUrl, '')) like concat('%', :term0, '%')
                or (:term1 is not null and (
                    coalesce(kc.content, '') like concat('%', :term1, '%')
                    or lower(coalesce(d.title, '')) like concat('%', :term1, '%')
                    or lower(coalesce(d.fileName, '')) like concat('%', :term1, '%')
                    or lower(coalesce(d.archiveEntryPath, '')) like concat('%', :term1, '%')
                    or lower(coalesce(d.sourceUrl, '')) like concat('%', :term1, '%')
                ))
                or (:term2 is not null and (
                    coalesce(kc.content, '') like concat('%', :term2, '%')
                    or lower(coalesce(d.title, '')) like concat('%', :term2, '%')
                    or lower(coalesce(d.fileName, '')) like concat('%', :term2, '%')
                    or lower(coalesce(d.archiveEntryPath, '')) like concat('%', :term2, '%')
                    or lower(coalesce(d.sourceUrl, '')) like concat('%', :term2, '%')
                ))
                or (:term3 is not null and (
                    coalesce(kc.content, '') like concat('%', :term3, '%')
                    or lower(coalesce(d.title, '')) like concat('%', :term3, '%')
                    or lower(coalesce(d.fileName, '')) like concat('%', :term3, '%')
                    or lower(coalesce(d.archiveEntryPath, '')) like concat('%', :term3, '%')
                    or lower(coalesce(d.sourceUrl, '')) like concat('%', :term3, '%')
                ))
                or (:term4 is not null and (
                    coalesce(kc.content, '') like concat('%', :term4, '%')
                    or lower(coalesce(d.title, '')) like concat('%', :term4, '%')
                    or lower(coalesce(d.fileName, '')) like concat('%', :term4, '%')
                    or lower(coalesce(d.archiveEntryPath, '')) like concat('%', :term4, '%')
                    or lower(coalesce(d.sourceUrl, '')) like concat('%', :term4, '%')
                ))
              )
        order by d.id asc, kc.chunkIndex asc, kc.id asc
        """)
    List<KnowledgeChunk> findKeywordCandidatesByKnowledgeBaseIds(
            @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
            @Param("term0") String term0,
            @Param("term1") String term1,
            @Param("term2") String term2,
            @Param("term3") String term3,
            @Param("term4") String term4,
            Pageable pageable
    );


    @Query("""
            select distinct kc
            from KnowledgeChunk kc
            join fetch kc.knowledgeBase kb
            join fetch kc.document d
            where kb.id in :knowledgeBaseIds
            order by
                case
                    when lower(coalesce(d.archiveEntryPath, d.sourceUrl, d.fileName, '')) like '%.vue' then 0
                    when lower(coalesce(d.archiveEntryPath, d.sourceUrl, d.fileName, '')) like '%.js' then 0
                    when lower(coalesce(d.archiveEntryPath, d.sourceUrl, d.fileName, '')) like '%.ts' then 0
                    when lower(coalesce(d.archiveEntryPath, d.sourceUrl, d.fileName, '')) like '%/ui/%' then 0
                    when lower(coalesce(d.archiveEntryPath, d.sourceUrl, d.fileName, '')) like '%/views/%' then 0
                    when lower(coalesce(d.archiveEntryPath, d.sourceUrl, d.fileName, '')) like '%/components/%' then 0
                    when lower(coalesce(d.archiveEntryPath, d.sourceUrl, d.fileName, '')) like '%/store/%' then 0
                    when lower(coalesce(d.archiveEntryPath, d.sourceUrl, d.fileName, '')) like '%/router/%' then 0
                    when lower(coalesce(d.archiveEntryPath, d.sourceUrl, d.fileName, '')) like '%/api/%' then 0
                    else 1
                end,
                d.id asc,
                kc.chunkIndex asc,
                kc.id asc
            """)
    List<KnowledgeChunk> findFrontendPreferredCandidatesByKnowledgeBaseIds(
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
