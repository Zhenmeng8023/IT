package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface KnowledgeImportTaskRepository extends JpaRepository<KnowledgeImportTask, Long> {

    List<KnowledgeImportTask> findByKnowledgeBase_IdOrderByCreatedAtDesc(Long knowledgeBaseId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update KnowledgeImportTask t " +
            "set t.status = :toStatus, " +
            "    t.currentStage = :stage, " +
            "    t.progressPercent = :progressPercent, " +
            "    t.currentFile = :currentFile, " +
            "    t.errorMessage = null, " +
            "    t.updatedAt = :updatedAt " +
            "where t.id = :taskId and t.status = :fromStatus")
    int transitionStatusToRunning(@Param("taskId") Long taskId,
                                  @Param("fromStatus") KnowledgeImportTask.Status fromStatus,
                                  @Param("toStatus") KnowledgeImportTask.Status toStatus,
                                  @Param("stage") KnowledgeImportTask.Stage stage,
                                  @Param("progressPercent") Integer progressPercent,
                                  @Param("currentFile") String currentFile,
                                  @Param("updatedAt") Instant updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update KnowledgeImportTask t " +
            "set t.status = :toStatus, " +
            "    t.currentStage = :stage, " +
            "    t.progressPercent = :progressPercent, " +
            "    t.currentFile = :currentFile, " +
            "    t.errorMessage = :errorMessage, " +
            "    t.finishedAt = :finishedAt, " +
            "    t.updatedAt = :updatedAt " +
            "where t.id = :taskId and t.status = :fromStatus")
    int transitionStatusFromRunning(@Param("taskId") Long taskId,
                                    @Param("fromStatus") KnowledgeImportTask.Status fromStatus,
                                    @Param("toStatus") KnowledgeImportTask.Status toStatus,
                                    @Param("stage") KnowledgeImportTask.Stage stage,
                                    @Param("progressPercent") Integer progressPercent,
                                    @Param("currentFile") String currentFile,
                                    @Param("errorMessage") String errorMessage,
                                    @Param("finishedAt") Instant finishedAt,
                                    @Param("updatedAt") Instant updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update KnowledgeImportTask t " +
            "set t.cancelRequested = true, " +
            "    t.updatedAt = :updatedAt " +
            "where t.id = :taskId and t.status in :activeStatuses")
    int requestCancel(@Param("taskId") Long taskId,
                      @Param("updatedAt") Instant updatedAt,
                      @Param("activeStatuses") List<KnowledgeImportTask.Status> activeStatuses);
}
