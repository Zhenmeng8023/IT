package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_activity_log")
public class ProjectActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "operator_id")
    private Long operatorId;
    @Column(nullable = false, length = 100)
    private String action;
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;
    @Column(name = "target_id")
    private Long targetId;
    @Column(name = "commit_id")
    private Long commitId;
    @Column(name = "branch_id")
    private Long branchId;
    @Column(name = "merge_request_id")
    private Long mergeRequestId;
    @Column(name = "check_run_id")
    private Long checkRunId;
    @Column(name = "summary", length = 255)
    private String content;
    @Column(name = "details", columnDefinition = "json")
    private String details;
    @Column(name = "extra_json", columnDefinition = "json")
    private String extraJson;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (content == null) content = "";
    }
}
