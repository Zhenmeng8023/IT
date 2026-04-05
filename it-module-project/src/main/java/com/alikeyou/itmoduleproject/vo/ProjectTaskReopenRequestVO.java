package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectTaskReopenRequestVO {
    private Long id;
    private Long taskId;
    private Long projectId;
    private Long applicantId;
    private String applicantName;
    private String applicantAvatar;
    private LocalDateTime applicantMemberJoinedAt;
    private String fromStatus;
    private String targetStatus;
    private String reason;
    private String status;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerAvatar;
    private LocalDateTime reviewedAt;
    private String reviewRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
