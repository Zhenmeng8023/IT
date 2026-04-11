package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_release")
public class ProjectRelease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "repository_id")
    private Long repositoryId;
    @Column(name = "branch_id")
    private Long branchId;
    @Column(name = "based_commit_id")
    private Long basedCommitId;
    @Column(name = "based_milestone_id")
    private Long basedMilestoneId;
    @Column(name = "package_blob_id")
    private Long packageBlobId;
    @Column(name = "recommended_flag")
    private Boolean recommendedFlag;
    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;
    @Column(nullable = false, length = 50)
    private String version;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "release_notes", columnDefinition = "longtext")
    private String releaseNotes;
    @Column(name = "release_type", nullable = false, length = 20)
    private String releaseType;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "published_by")
    private Long publishedBy;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (releaseType == null || releaseType.isBlank()) releaseType = "draft";
        if (status == null || status.isBlank()) status = "draft";
        if (recommendedFlag == null) recommendedFlag = false;
    }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
