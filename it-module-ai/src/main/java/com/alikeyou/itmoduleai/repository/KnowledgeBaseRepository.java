package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    Page<KnowledgeBase> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<KnowledgeBase> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId, Pageable pageable);

    Page<KnowledgeBase> findByProjectIdOrderByUpdatedAtDesc(Long projectId, Pageable pageable);

    Page<KnowledgeBase> findByOwnerIdAndProjectIdOrderByUpdatedAtDesc(Long ownerId,
                                                                      Long projectId,
                                                                      Pageable pageable);

    Page<KnowledgeBase> findByScopeTypeOrderByUpdatedAtDesc(KnowledgeBase.ScopeType scopeType, Pageable pageable);

    Page<KnowledgeBase> findByScopeTypeAndOwnerIdOrderByUpdatedAtDesc(KnowledgeBase.ScopeType scopeType,
                                                                      Long ownerId,
                                                                      Pageable pageable);

    Page<KnowledgeBase> findByScopeTypeAndProjectIdOrderByUpdatedAtDesc(KnowledgeBase.ScopeType scopeType,
                                                                        Long projectId,
                                                                        Pageable pageable);

    Page<KnowledgeBase> findByScopeTypeAndOwnerIdAndProjectIdOrderByUpdatedAtDesc(KnowledgeBase.ScopeType scopeType,
                                                                                  Long ownerId,
                                                                                  Long projectId,
                                                                                  Pageable pageable);

    @Query("""
            select d.storagePath
            from KnowledgeDocument d
            where d.knowledgeBase.id = :knowledgeBaseId
              and d.storagePath is not null
              and d.storagePath <> ''
            order by d.id asc
            """)
    List<String> findDocumentStoragePaths(@Param("knowledgeBaseId") Long knowledgeBaseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from KnowledgeBase kb where kb.id = :knowledgeBaseId")
    int deleteByIdInBulk(@Param("knowledgeBaseId") Long knowledgeBaseId);

    @Query("""
            select kb
            from KnowledgeBase kb
            where kb.scopeType = com.alikeyou.itmoduleai.entity.KnowledgeBase$ScopeType.PROJECT
              and kb.projectId = :projectId
              and (
                    kb.ownerId = :userId
                    or kb.visibility = com.alikeyou.itmoduleai.entity.KnowledgeBase$Visibility.PUBLIC
                    or kb.visibility = com.alikeyou.itmoduleai.entity.KnowledgeBase$Visibility.TEAM
                    or exists (
                        select 1
                        from KnowledgeBaseMember m
                        where m.knowledgeBase.id = kb.id
                          and m.userId = :userId
                    )
                  )
            order by kb.updatedAt desc
            """)
    Page<KnowledgeBase> findAccessibleProjectPage(@Param("projectId") Long projectId,
                                                  @Param("userId") Long userId,
                                                  Pageable pageable);

    @Query(value = """
            select case when count(*) > 0 then true else false end
            from project p
            left join project_member pm
              on pm.project_id = p.id
             and pm.user_id = :userId
             and lower(pm.status) = 'active'
            where p.id = :projectId
              and (p.author_id = :userId or pm.id is not null)
            """, nativeQuery = true)
    boolean existsAccessibleProject(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
