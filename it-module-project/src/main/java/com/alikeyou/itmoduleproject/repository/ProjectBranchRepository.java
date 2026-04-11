package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectBranchRepository extends JpaRepository<ProjectBranch, Long> {
    List<ProjectBranch> findByRepositoryIdOrderByCreatedAtAsc(Long repositoryId);
    Optional<ProjectBranch> findByRepositoryIdAndName(Long repositoryId, String name);
}
