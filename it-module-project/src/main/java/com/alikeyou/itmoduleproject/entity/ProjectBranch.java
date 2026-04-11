package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_branch")
public class ProjectBranch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "head_commit_id")
    private Long headCommitId;
    @Column(name = "branch_type", nullable = false, length = 20)
    private String branchType;
    @Column(name = "protected_flag", nullable = false)
    private Boolean protectedFlag;
    @Column(name = "allow_direct_commit_flag", nullable = false)
    private Boolean allowDirectCommitFlag;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @PrePersist public void prePersist() {
        if (branchType == null || branchType.isBlank()) branchType = "feature";
        if (protectedFlag == null) protectedFlag = false;
        if (allowDirectCommitFlag == null) allowDirectCommitFlag = false;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
