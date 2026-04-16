package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectWorkspaceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectWorkspaceItemRepository extends JpaRepository<ProjectWorkspaceItem, Long> {
    List<ProjectWorkspaceItem> findByWorkspaceIdOrderByIdAsc(Long workspaceId);
    Optional<ProjectWorkspaceItem> findFirstByWorkspaceIdAndCanonicalPath(Long workspaceId, String canonicalPath);

    @Modifying
    @Query(value = """
            INSERT INTO project_workspace_item
            (workspace_id, canonical_path, temp_storage_path, blob_id, change_type, staged_flag, conflict_flag, detected_message)
            VALUES
            (:workspaceId, :canonicalPath, :tempStoragePath, :blobId, :changeType, :stagedFlag, :conflictFlag, :detectedMessage)
            ON DUPLICATE KEY UPDATE
                id = LAST_INSERT_ID(id),
                temp_storage_path = VALUES(temp_storage_path),
                blob_id = VALUES(blob_id),
                change_type = VALUES(change_type),
                staged_flag = VALUES(staged_flag),
                conflict_flag = VALUES(conflict_flag),
                detected_message = VALUES(detected_message)
            """, nativeQuery = true)
    int upsertByWorkspaceAndPath(@Param("workspaceId") Long workspaceId,
                                 @Param("canonicalPath") String canonicalPath,
                                 @Param("tempStoragePath") String tempStoragePath,
                                 @Param("blobId") Long blobId,
                                 @Param("changeType") String changeType,
                                 @Param("stagedFlag") Boolean stagedFlag,
                                 @Param("conflictFlag") Boolean conflictFlag,
                                 @Param("detectedMessage") String detectedMessage);

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    Long selectLastInsertId();

    long deleteByWorkspaceId(Long workspaceId);

    long deleteByWorkspaceIdAndCanonicalPath(Long workspaceId, String canonicalPath);
}
