package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_workspace")
public class ProjectWorkspace {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;
    @Column(name = "branch_id", nullable = false)
    private Long branchId;
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    @Column(name = "base_commit_id", nullable = false)
    private Long baseCommitId;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null || status.isBlank()) status = "active";
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
