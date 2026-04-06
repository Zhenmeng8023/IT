package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class BlogAuditHistoryVO {
    private Long id;
    private Long blogId;
    private String auditType;
    private String auditStatus;
    private String auditReason;
    private String auditComment;
    private Long auditorId;
    private String auditorName;
    private Instant createdAt;
    private LocalDateTime reviewedAt;
}
