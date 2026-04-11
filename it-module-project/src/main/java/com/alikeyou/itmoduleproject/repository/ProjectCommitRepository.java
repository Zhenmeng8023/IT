package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectCommitRepository extends JpaRepository<ProjectCommit, Long> {
    List<ProjectCommit> findByBranchIdOrderByCreatedAtDesc(Long branchId);
    Optional<ProjectCommit> findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(Long repositoryId, Long branchId);
}
