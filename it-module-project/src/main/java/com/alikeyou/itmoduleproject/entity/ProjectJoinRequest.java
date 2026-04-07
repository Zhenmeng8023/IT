
package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_join_request")
public class ProjectJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "desired_role", nullable = false, length = 20)
    private String desiredRole;

    @Column(name = "apply_message", length = 500)
    private String applyMessage;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "review_message", length = 500)
    private String reviewMessage;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null || status.isBlank()) {
            status = "pending";
        }
        if (desiredRole == null || desiredRole.isBlank()) {
            desiredRole = "member";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (desiredRole == null || desiredRole.isBlank()) {
            desiredRole = "member";
        }
    }
}
