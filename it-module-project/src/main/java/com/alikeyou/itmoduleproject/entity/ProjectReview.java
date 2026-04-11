package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_review")
public class ProjectReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "merge_request_id", nullable = false)
    private Long mergeRequestId;
    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;
    @Column(name = "review_result", nullable = false, length = 20)
    private String reviewResult;
    @Column(name = "review_comment", length = 1000)
    private String reviewComment;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
