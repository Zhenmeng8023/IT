package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    Page<KnowledgeBase> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<KnowledgeBase> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId, Pageable pageable);

    Page<KnowledgeBase> findByProjectIdOrderByUpdatedAtDesc(Long projectId, Pageable pageable);

    @Query("""
            select kb
            from KnowledgeBase kb
            where kb.projectId = :projectId
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
}
