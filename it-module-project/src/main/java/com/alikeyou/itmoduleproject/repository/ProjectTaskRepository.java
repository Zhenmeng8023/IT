package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long>, JpaSpecificationExecutor<ProjectTask> {
    List<ProjectTask> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ProjectTask> findByProjectIdAndAssigneeIdOrderByCreatedAtDesc(Long projectId, Long assigneeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ProjectTask t
               set t.assigneeId = null,
                   t.status = :todoStatus,
                   t.completedAt = null,
                   t.updatedAt = CURRENT_TIMESTAMP
             where t.projectId = :projectId
               and t.assigneeId = :assigneeId
               and t.status <> :doneStatus
            """)
    int unassignActiveTasksByProjectAndAssignee(
            @Param("projectId") Long projectId,
            @Param("assigneeId") Long assigneeId,
            @Param("todoStatus") String todoStatus,
            @Param("doneStatus") String doneStatus
    );
}
