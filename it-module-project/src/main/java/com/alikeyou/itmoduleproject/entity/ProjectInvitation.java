
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
@Table(name = "project_invitation")
public class ProjectInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;

    @Column(name = "invitee_id")
    private Long inviteeId;

    @Column(name = "invitee_email", length = 100)
    private String inviteeEmail;

    @Column(name = "invite_role", nullable = false, length = 20)
    private String inviteRole;

    @Column(name = "invite_code", nullable = false, length = 64)
    private String inviteCode;

    @Column(name = "invite_message", length = 500)
    private String inviteMessage;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

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
        if (inviteRole == null || inviteRole.isBlank()) {
            inviteRole = "member";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == null || status.isBlank()) {
            status = "pending";
        }
        if (inviteRole == null || inviteRole.isBlank()) {
            inviteRole = "member";
        }
    }
}
