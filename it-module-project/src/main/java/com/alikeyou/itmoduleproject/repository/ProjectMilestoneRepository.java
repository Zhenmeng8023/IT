package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {
    List<ProjectMilestone> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<ProjectMilestone> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, String status);
    long countByProjectId(Long projectId);
    long countByProjectIdAndStatus(Long projectId, String status);
    Optional<ProjectMilestone> findFirstByProjectIdAndStatusOrderByDueDateAscIdAsc(Long projectId, String status);
}
