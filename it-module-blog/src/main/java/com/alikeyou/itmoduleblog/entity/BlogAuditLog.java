
package com.alikeyou.itmoduleblog.entity;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "blog_audit_log", schema = "it9_data")
public class BlogAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "blog_id", nullable = false)
    private Long blogId;

    @Column(name = "audit_type", nullable = false, length = 20)
    private String auditType;

    @Column(name = "audit_status", nullable = false, length = 20)
    private String auditStatus;

    @Column(name = "audit_score", precision = 5, scale = 2)
    private BigDecimal auditScore;

    @Lob
    @Column(name = "audit_reason")
    private String auditReason;

    @Column(name = "auto_review_suggestion", length = 200)
    private String autoReviewSuggestion;

    @Column(name = "requires_manual_review", nullable = false)
    private Boolean requiresManualReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id")
    private UserInfo auditor;

    @Column(name = "audit_comment", length = 500)
    private String auditComment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
