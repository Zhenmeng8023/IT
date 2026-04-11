package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_workspace_item")
public class ProjectWorkspaceItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;
    @Column(name = "canonical_path", nullable = false, length = 1000)
    private String canonicalPath;
    @Column(name = "temp_storage_path", length = 1000)
    private String tempStoragePath;
    @Column(name = "blob_id")
    private Long blobId;
    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;
    @Column(name = "staged_flag", nullable = false)
    private Boolean stagedFlag;
    @Column(name = "conflict_flag", nullable = false)
    private Boolean conflictFlag;
    @Column(name = "detected_message", length = 500)
    private String detectedMessage;
    @PrePersist public void prePersist() {
        if (stagedFlag == null) stagedFlag = true;
        if (conflictFlag == null) conflictFlag = false;
    }
}
