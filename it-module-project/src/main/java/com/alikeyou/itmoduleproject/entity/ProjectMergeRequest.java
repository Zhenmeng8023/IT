package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_merge_request")
public class ProjectMergeRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;
    @Column(name = "source_branch_id", nullable = false)
    private Long sourceBranchId;
    @Column(name = "target_branch_id", nullable = false)
    private Long targetBranchId;
    @Column(name = "source_head_commit_id", nullable = false)
    private Long sourceHeadCommitId;
    @Column(name = "target_head_commit_id", nullable = false)
    private Long targetHeadCommitId;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "merged_by")
    private Long mergedBy;
    @Column(name = "merged_at")
    private LocalDateTime mergedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist public void prePersist() {
        if (status == null || status.isBlank()) status = "open";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
