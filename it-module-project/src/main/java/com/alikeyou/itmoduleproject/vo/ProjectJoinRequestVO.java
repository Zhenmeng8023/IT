
package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectJoinRequestVO {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long applicantId;
    private String applicantName;
    private String desiredRole;
    private String applyMessage;
    private String status;
    private Long reviewerId;
    private String reviewerName;
    private String reviewMessage;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
