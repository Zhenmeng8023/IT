package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectSprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectSprintRepository extends JpaRepository<ProjectSprint, Long> {

    List<ProjectSprint> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ProjectSprint> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, String status);

    Optional<ProjectSprint> findFirstByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, String status);
}
