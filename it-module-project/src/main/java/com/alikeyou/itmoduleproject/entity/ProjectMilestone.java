package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_milestone")
public class ProjectMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "repository_id")
    private Long repositoryId;
    @Column(name = "branch_id")
    private Long branchId;
    @Column(name = "anchor_commit_id")
    private Long anchorCommitId;
    @Column(name = "from_commit_id")
    private Long fromCommitId;
    @Column(name = "to_commit_id")
    private Long toCommitId;
    @Column(name = "sort_order")
    private Integer sortOrder;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "due_date")
    private LocalDate dueDate;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null || status.isBlank()) status = "planned";
        if (sortOrder == null) sortOrder = 0;
        if ("completed".equals(status) && completedAt == null) completedAt = now;
    }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
