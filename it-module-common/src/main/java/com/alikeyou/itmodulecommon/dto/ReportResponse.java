package com.alikeyou.itmodulecommon.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private Long processorId;
    private String processorName;
    private String targetType;
    private Long targetId;
    private String reason;
    private String status;
    private Instant createdAt;
    private Instant processedAt;
}
