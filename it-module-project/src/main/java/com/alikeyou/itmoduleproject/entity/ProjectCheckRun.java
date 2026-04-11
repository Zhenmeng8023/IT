package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_check_run")
public class ProjectCheckRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;
    @Column(name = "commit_id")
    private Long commitId;
    @Column(name = "merge_request_id")
    private Long mergeRequestId;
    @Column(name = "check_type", nullable = false, length = 30)
    private String checkType;
    @Column(name = "check_status", nullable = false, length = 20)
    private String checkStatus;
    @Column(length = 500)
    private String summary;
    @Column(name = "log_path", length = 1000)
    private String logPath;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist public void prePersist() {
        if (checkStatus == null || checkStatus.isBlank()) checkStatus = "queued";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
