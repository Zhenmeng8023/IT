package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_stat_daily")
public class ProjectStatDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(name = "view_count", nullable = false)
    private Integer viewCount;
    @Column(name = "unique_visitor_count", nullable = false)
    private Integer uniqueVisitorCount;
    @Column(name = "download_count", nullable = false)
    private Integer downloadCount;
    @Column(name = "unique_download_user_count", nullable = false)
    private Integer uniqueDownloadUserCount;
    @Column(name = "commit_count", nullable = false)
    private Integer commitCount;
    @Column(name = "merge_count", nullable = false)
    private Integer mergeCount;
    @Column(name = "rollback_count", nullable = false)
    private Integer rollbackCount;
    @Column(name = "conflict_count", nullable = false)
    private Integer conflictCount;
    @Column(name = "release_count", nullable = false)
    private Integer releaseCount;
    @Column(name = "star_count", nullable = false)
    private Integer starCount;
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount;
    @Column(name = "member_active_count", nullable = false)
    private Integer memberActiveCount;
    @Column(name = "new_member_count", nullable = false)
    private Integer newMemberCount;
    @Column(name = "task_created_count", nullable = false)
    private Integer taskCreatedCount;
    @Column(name = "task_completed_count", nullable = false)
    private Integer taskCompletedCount;
    @Column(name = "revenue_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal revenueAmount;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (viewCount == null) viewCount = 0;
        if (uniqueVisitorCount == null) uniqueVisitorCount = 0;
        if (downloadCount == null) downloadCount = 0;
        if (uniqueDownloadUserCount == null) uniqueDownloadUserCount = 0;
        if (commitCount == null) commitCount = 0;
        if (mergeCount == null) mergeCount = 0;
        if (rollbackCount == null) rollbackCount = 0;
        if (conflictCount == null) conflictCount = 0;
        if (releaseCount == null) releaseCount = 0;
        if (starCount == null) starCount = 0;
        if (commentCount == null) commentCount = 0;
        if (memberActiveCount == null) memberActiveCount = 0;
        if (newMemberCount == null) newMemberCount = 0;
        if (taskCreatedCount == null) taskCreatedCount = 0;
        if (taskCompletedCount == null) taskCompletedCount = 0;
        if (revenueAmount == null) revenueAmount = BigDecimal.ZERO;
    }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
