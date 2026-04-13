package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiCodeSymbol;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiCodeSymbolRepository extends JpaRepository<AiCodeSymbol, Long> {

    List<AiCodeSymbol> findByDocument_IdOrderByStartLineAsc(Long documentId);

    List<AiCodeSymbol> findByKnowledgeBase_IdAndFilePathOrderByStartLineAsc(Long knowledgeBaseId, String filePath);

    Optional<AiCodeSymbol> findBySymbolKey(String symbolKey);

    @Query("""
            select distinct s
            from AiCodeSymbol s
            join fetch s.knowledgeBase kb
            join fetch s.document d
            left join fetch s.chunk c
            where kb.id in :knowledgeBaseIds
              and s.status = com.alikeyou.itmoduleai.entity.AiCodeSymbol.Status.ACTIVE
              and s.isDeclaration = true
              and (
                    lower(coalesce(s.symbolName, '')) like concat('%', :term0, '%')
                    or lower(coalesce(s.qualifiedName, '')) like concat('%', :term0, '%')
                    or (:term1 is not null and (
                        lower(coalesce(s.symbolName, '')) like concat('%', :term1, '%')
                        or lower(coalesce(s.qualifiedName, '')) like concat('%', :term1, '%')
                    ))
                    or (:term2 is not null and (
                        lower(coalesce(s.symbolName, '')) like concat('%', :term2, '%')
                        or lower(coalesce(s.qualifiedName, '')) like concat('%', :term2, '%')
                    ))
                    or (:term3 is not null and (
                        lower(coalesce(s.symbolName, '')) like concat('%', :term3, '%')
                        or lower(coalesce(s.qualifiedName, '')) like concat('%', :term3, '%')
                    ))
                    or (:term4 is not null and (
                        lower(coalesce(s.symbolName, '')) like concat('%', :term4, '%')
                        or lower(coalesce(s.qualifiedName, '')) like concat('%', :term4, '%')
                    ))
              )
            order by d.id asc, s.startLine asc, s.id asc
            """)
    List<AiCodeSymbol> findDeclarationCandidatesByKnowledgeBaseIds(
            @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
            @Param("term0") String term0,
            @Param("term1") String term1,
            @Param("term2") String term2,
            @Param("term3") String term3,
            @Param("term4") String term4,
            Pageable pageable
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AiCodeSymbol s
            where s.document.id = :documentId
            """)
    void deleteByDocumentId(@Param("documentId") Long documentId);
}
