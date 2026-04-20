package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectWorkspaceRepository extends JpaRepository<ProjectWorkspace, Long> {
    Optional<ProjectWorkspace> findFirstByRepositoryIdAndBranchIdAndOwnerIdAndStatusOrderByUpdatedAtDesc(
            Long repositoryId, Long branchId, Long ownerId, String status
    );
    List<ProjectWorkspace> findByRepositoryId(Long repositoryId);

    @Modifying
    @Query(value = """
            INSERT INTO project_workspace
                (repository_id, branch_id, owner_id, active_owner_id, base_commit_id, status)
            VALUES
                (:repositoryId, :branchId, :ownerId, NULL, :baseCommitId, 'active')
            ON DUPLICATE KEY UPDATE
                id = LAST_INSERT_ID(id),
                updated_at = updated_at
            """, nativeQuery = true)
    int upsertActiveWorkspace(@Param("repositoryId") Long repositoryId,
                              @Param("branchId") Long branchId,
                              @Param("ownerId") Long ownerId,
                              @Param("baseCommitId") Long baseCommitId);

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    Long selectLastInsertId();
}
