package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    Page<KnowledgeBase> findByOwner_Id(Long ownerId, Pageable pageable);

    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.owner.id = :projectId")
    Page<KnowledgeBase> findByProjectId(@Param("projectId") Long projectId, Pageable pageable);

    List<KnowledgeBase> findByScopeTypeAndStatus(KnowledgeBase.ScopeType scopeType, KnowledgeBase.Status status);

    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.owner.id = :userId AND kb.status = :status")
    List<KnowledgeBase> findUserKnowledgeBases(@Param("userId") Long userId,
                                               @Param("status") KnowledgeBase.Status status);
}
