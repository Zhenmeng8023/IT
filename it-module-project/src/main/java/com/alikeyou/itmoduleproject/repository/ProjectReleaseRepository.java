package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectRelease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectReleaseRepository extends JpaRepository<ProjectRelease, Long> {
    List<ProjectRelease> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<ProjectRelease> findByProjectIdAndStatusOrderByPublishedAtDescIdDesc(Long projectId, String status);
    boolean existsByProjectIdAndVersion(Long projectId, String version);
    Optional<ProjectRelease> findFirstByProjectIdAndStatusOrderByPublishedAtDescIdDesc(Long projectId, String status);
}
