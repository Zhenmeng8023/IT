package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_commit")
public class ProjectCommit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;
    @Column(name = "branch_id", nullable = false)
    private Long branchId;
    @Column(name = "commit_no", nullable = false)
    private Long commitNo;
    @Column(name = "display_sha", length = 40)
    private String displaySha;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "commit_type", nullable = false, length = 20)
    private String commitType;
    @Column(name = "snapshot_id")
    private Long snapshotId;
    @Column(name = "operator_id")
    private Long operatorId;
    @Column(name = "base_commit_id")
    private Long baseCommitId;
    @Column(name = "is_merge_commit", nullable = false)
    private Boolean isMergeCommit;
    @Column(name = "is_revert_commit", nullable = false)
    private Boolean isRevertCommit;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist public void prePersist() {
        if (commitType == null || commitType.isBlank()) commitType = "normal";
        if (isMergeCommit == null) isMergeCommit = false;
        if (isRevertCommit == null) isRevertCommit = false;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
