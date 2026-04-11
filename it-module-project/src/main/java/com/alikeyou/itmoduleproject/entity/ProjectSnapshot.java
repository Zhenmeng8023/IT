package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_snapshot")
public class ProjectSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;
    @Column(name = "commit_id")
    private Long commitId;
    @Column(name = "manifest_hash", length = 64)
    private String manifestHash;
    @Column(name = "file_count", nullable = false)
    private Integer fileCount;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @PrePersist public void prePersist() {
        if (fileCount == null) fileCount = 0;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
