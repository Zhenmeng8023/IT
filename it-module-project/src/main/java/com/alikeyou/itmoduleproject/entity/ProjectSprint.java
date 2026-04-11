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
@Table(name = "project_sprint")
public class ProjectSprint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "repository_id")
    private Long repositoryId;
    @Column(name = "branch_id")
    private Long branchId;
    @Column(name = "start_commit_id")
    private Long startCommitId;
    @Column(name = "end_commit_id")
    private Long endCommitId;
    @Column(name = "planned_points")
    private Integer plannedPoints;
    @Column(name = "completed_points")
    private Integer completedPoints;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(columnDefinition = "text")
    private String goal;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
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
        if (plannedPoints == null) plannedPoints = 0;
        if (completedPoints == null) completedPoints = 0;
    }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
