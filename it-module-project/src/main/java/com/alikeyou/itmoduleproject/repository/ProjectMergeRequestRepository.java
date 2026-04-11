package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectMergeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMergeRequestRepository extends JpaRepository<ProjectMergeRequest, Long> {
    List<ProjectMergeRequest> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId);
    List<ProjectMergeRequest> findByRepositoryIdAndStatusOrderByCreatedAtDesc(Long repositoryId, String status);
}
