package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectWorkspaceRepository extends JpaRepository<ProjectWorkspace, Long> {
    Optional<ProjectWorkspace> findFirstByRepositoryIdAndBranchIdAndOwnerIdAndStatusOrderByUpdatedAtDesc(
            Long repositoryId, Long branchId, Long ownerId, String status
    );
}
