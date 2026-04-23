package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeIndexTaskRepository extends JpaRepository<KnowledgeIndexTask, Long> {

    List<KnowledgeIndexTask> findByStatusOrderByCreatedAtAsc(KnowledgeIndexTask.Status status);

    List<KnowledgeIndexTask> findByDocument_IdOrderByCreatedAtDesc(Long documentId);

    List<KnowledgeIndexTask> findByKnowledgeBase_IdOrderByCreatedAtDesc(Long knowledgeBaseId);

    @Query("select t from KnowledgeIndexTask t " +
            "join fetch t.knowledgeBase kb " +
            "left join fetch t.document d " +
            "where t.id = :taskId")
    Optional<KnowledgeIndexTask> findByIdWithRelations(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update KnowledgeIndexTask t " +
            "set t.status = :toStatus, " +
            "    t.startedAt = :startedAt, " +
            "    t.errorMessage = null, " +
            "    t.updatedAt = :updatedAt " +
            "where t.id = :taskId and t.status = :fromStatus")
    int transitionStatusToRunning(@Param("taskId") Long taskId,
                                  @Param("fromStatus") KnowledgeIndexTask.Status fromStatus,
                                  @Param("toStatus") KnowledgeIndexTask.Status toStatus,
                                  @Param("startedAt") Instant startedAt,
                                  @Param("updatedAt") Instant updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update KnowledgeIndexTask t " +
            "set t.status = :toStatus, " +
            "    t.errorMessage = :errorMessage, " +
            "    t.finishedAt = :finishedAt, " +
            "    t.updatedAt = :updatedAt " +
            "where t.id = :taskId and t.status = :fromStatus")
    int transitionStatusFromRunning(@Param("taskId") Long taskId,
                                    @Param("fromStatus") KnowledgeIndexTask.Status fromStatus,
                                    @Param("toStatus") KnowledgeIndexTask.Status toStatus,
                                    @Param("errorMessage") String errorMessage,
                                    @Param("finishedAt") Instant finishedAt,
                                    @Param("updatedAt") Instant updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update KnowledgeIndexTask t " +
            "set t.status = :toStatus, " +
            "    t.errorMessage = :errorMessage, " +
            "    t.startedAt = null, " +
            "    t.finishedAt = null, " +
            "    t.updatedAt = :updatedAt " +
            "where t.id = :taskId and t.status = :fromStatus")
    int resetStatus(@Param("taskId") Long taskId,
                    @Param("fromStatus") KnowledgeIndexTask.Status fromStatus,
                    @Param("toStatus") KnowledgeIndexTask.Status toStatus,
                    @Param("errorMessage") String errorMessage,
                    @Param("updatedAt") Instant updatedAt);
}
