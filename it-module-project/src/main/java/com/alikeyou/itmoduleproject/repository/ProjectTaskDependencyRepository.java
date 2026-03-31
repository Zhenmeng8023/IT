package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTaskDependencyRepository extends JpaRepository<ProjectTaskDependency, Long> {
    List<ProjectTaskDependency> findBySuccessorTaskId(Long successorTaskId);

    List<ProjectTaskDependency> findByPredecessorTaskId(Long predecessorTaskId);

    boolean existsByPredecessorTaskIdAndSuccessorTaskId(Long predecessorTaskId, Long successorTaskId);
}
