package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long>, JpaSpecificationExecutor<ProjectTask> {
    List<ProjectTask> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ProjectTask> findByProjectIdAndAssigneeIdOrderByCreatedAtDesc(Long projectId, Long assigneeId);
}
