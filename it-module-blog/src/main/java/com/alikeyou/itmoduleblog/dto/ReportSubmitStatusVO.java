package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ReportSubmitStatusVO {
    private Boolean reported;
    private String lastReportStatus;
    private Instant lastReportTime;
}
